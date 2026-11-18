package com.ai.epiphcode.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.ai.epiphcode.ai.AiCodeGenTypeRoutingService;
import com.ai.epiphcode.constant.AppConstant;
import com.ai.epiphcode.constant.UserConstant;
import com.ai.epiphcode.core.engine.ChatCodeGenerationEngine;
import com.ai.epiphcode.core.handler.TurnAccumulatorManager;
import com.ai.epiphcode.core.handler.StreamHandlerExecutor;
import com.ai.epiphcode.exception.BusinessException;
import com.ai.epiphcode.exception.ErrorCode;
import com.ai.epiphcode.exception.ThrowUtils;
import com.ai.epiphcode.mapper.AppMapper;
import com.ai.epiphcode.model.dto.app.AppAddRequest;
import com.ai.epiphcode.model.dto.app.AppQueryRequest;
import com.ai.epiphcode.model.entity.App;
import com.ai.epiphcode.model.entity.User;
import com.ai.epiphcode.model.enums.ChatAgentEnum;
import com.ai.epiphcode.model.enums.ChatGenModeEnum;
import com.ai.epiphcode.model.enums.CodeGenTypeEnum;
import com.ai.epiphcode.model.vo.AppVO;
import com.ai.epiphcode.model.vo.UserVO;
import com.ai.epiphcode.service.AppService;
import com.ai.epiphcode.service.ChatEventLogService;
import com.ai.epiphcode.service.ChatHistoryService;
import com.ai.epiphcode.service.ScreenshotService;
import com.ai.epiphcode.service.impl.AppVersionService;
import com.ai.epiphcode.service.UserService;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import reactor.core.publisher.Flux;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 应用 服务层实现。
 *
 * @author Luo QinFeng
 */
@Service
@Slf4j
public class AppServiceImpl extends ServiceImpl<AppMapper, App>  implements AppService {
    private static final String APP_CHAT_MODE_LOCK_KEY = "app:chat:mode:lock:%d";
    private static final String MODE_WORKFLOW = "workflow";

    @Resource
    private UserService userService;
    @Resource
    private List<ChatCodeGenerationEngine> chatCodeGenerationEngines;
    @Resource
    private StreamHandlerExecutor streamHandlerExecutor;
    @Resource
    private ChatHistoryService chatHistoryService;
    @Resource
    private ChatEventLogService chatEventLogService;
    @Resource
    private TurnAccumulatorManager turnAccumulatorManager;
    @Resource
    private ScreenshotService screenshotService;
    @Resource
    private AppVersionService appVersionService;

    @Resource
    private AiCodeGenTypeRoutingService aiCodeGenTypeRoutingService;
    @Resource
    private CacheManager cacheManager;
    @Resource
    private RedissonClient redissonClient;

    private final Map<ChatGenModeEnum, ChatCodeGenerationEngine> generationEngineMap =
            new EnumMap<>(ChatGenModeEnum.class);

    @Override
    public AppVO getAppVO(App app) {
        if (app == null) {
            return null;
        }
        AppVO appVO = toAppVOBase(app);
        // 关联查询用户信息
        Long userId = app.getUserId();
        if (userId != null) {
            User user = userService.getById(userId);
            UserVO userVO = userService.getUserVO(user);
            appVO.setUser(userVO);
        }
        appVersionService.fillVersionNumbers(List.of(app), List.of(appVO));
        return appVO;
    }

    private AppVO toAppVOBase(App app) {
        AppVO appVO = new AppVO();
        BeanUtil.copyProperties(app, appVO);
        appVO.setWorkingVersionId(app.getWorkingVersionId() == null ? null : String.valueOf(app.getWorkingVersionId()));
        appVO.setDeployedVersionId(app.getDeployedVersionId() == null ? null : String.valueOf(app.getDeployedVersionId()));
        return appVO;
    }

    @Override
    public QueryWrapper getQueryWrapper(AppQueryRequest appQueryRequest) {
        if (appQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        Long id = appQueryRequest.getId();
        String appName = appQueryRequest.getAppName();
        String cover = appQueryRequest.getCover();
        String initPrompt = appQueryRequest.getInitPrompt();
        String codeGenType = appQueryRequest.getCodeGenType();
        String deployKey = appQueryRequest.getDeployKey();
        Integer priority = appQueryRequest.getPriority();
        Long userId = appQueryRequest.getUserId();
        String sortField = appQueryRequest.getSortField();
        String sortOrder = appQueryRequest.getSortOrder();
        return QueryWrapper.create()
                .eq("id", id)
                .like("appName", appName)
                .like("cover", cover)
                .like("initPrompt", initPrompt)
                .eq("codeGenType", codeGenType)
                .eq("deployKey", deployKey)
                .eq("priority", priority)
                .eq("userId", userId)
                .orderBy(sortField, "ascend".equals(sortOrder));
    }

    @Override
    public List<AppVO> getAppVOList(List<App> appList) {
        if (CollUtil.isEmpty(appList)) {
            return new ArrayList<>();
        }
        // 批量获取用户信息，避免 N+1 查询问题  对每个 App 都去查一次用户，5 条记录就要查 5 次数据库；现在只需要 1 次批量查询。
        Set<Long> userIds = appList.stream()
                .map(App::getUserId)
                .collect(Collectors.toSet());
        Map<Long, UserVO> userVOMap = userService.listByIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, userService::getUserVO));
        List<AppVO> appVOList = appList.stream().map(app -> {
            AppVO appVO = toAppVOBase(app);
            UserVO userVO = userVOMap.get(app.getUserId());
            appVO.setUser(userVO);
            return appVO;
        }).collect(Collectors.toList());
        appVersionService.fillVersionNumbers(appList, appVOList);
        return appVOList;
    }


    @Override
    public Flux<String> chatToGenCode(Long appId, String message, User loginUser, String mode, String agent) {
        return chatToGenCode(appId, message, loginUser, mode, agent, null);
    }

    @Override
    public Flux<String> chatToGenCode(Long appId, String message, User loginUser, String mode, String agent, Long baseVersionId) {
        // 1. 参数校验
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用 ID 不能为空");
        ThrowUtils.throwIf(StrUtil.isBlank(message), ErrorCode.PARAMS_ERROR, "用户消息不能为空");
        // 2. 查询应用信息
        App app = this.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        // 3. 验证用户是否有权限访问该应用，仅本人可以生成代码
        if (!app.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限访问该应用");
        }
        // 4. 获取应用的代码生成类型
        String codeGenTypeStr = app.getCodeGenType();
        CodeGenTypeEnum codeGenTypeEnum = CodeGenTypeEnum.getEnumByValue(codeGenTypeStr);
        if (codeGenTypeEnum == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "不支持的代码生成类型");
        }
        ChatGenModeEnum chatGenMode = ChatGenModeEnum.getByValue(mode);
        ChatAgentEnum chatAgent = ChatAgentEnum.getByValue(agent);
        ThrowUtils.throwIf(chatAgent == null, ErrorCode.PARAMS_ERROR, "不支持的 Agent");
        // 一旦某应用进入 workflow，后续请求强制 workflow，避免模式来回切换污染会话
        if (isWorkflowLocked(appId)) {
            chatGenMode = ChatGenModeEnum.WORKFLOW;
        } else if (chatGenMode == ChatGenModeEnum.WORKFLOW) {
            lockWorkflowMode(appId);
        }
        ChatCodeGenerationEngine generationEngine = getGenerationEngine(chatGenMode);
        String turnId = IdUtil.fastSimpleUUID();
        String memoryId = appId + "_" + codeGenTypeEnum.getValue();
        Long resolvedBaseVersionId = appVersionService.beginTurn(app, loginUser, baseVersionId);
        // 5. 初始化本轮聚合上下文（流式阶段仅聚合，不落库）
        turnAccumulatorManager.startTurn(
                appId, loginUser.getId(), memoryId, turnId, codeGenTypeEnum.getValue(), message,
                chatAgent.getValue(), resolvedBaseVersionId);
        try {
            // 6. 根据模式选择生成引擎；快照锁在流完成或失败时释放。
            Flux<String> contentFlux = generationEngine.generate(appId, message, loginUser, codeGenTypeEnum, chatAgent, turnId);
            return streamHandlerExecutor.doExecute(contentFlux, appId, loginUser, codeGenTypeEnum, turnId);
        } catch (RuntimeException e) {
            try {
                appVersionService.rollbackTurn(turnAccumulatorManager.getRequired(turnId));
            } finally {
                appVersionService.finishTurn(turnAccumulatorManager.getRequired(turnId));
            }
            turnAccumulatorManager.remove(turnId);
            throw e;
        }
    }

    private ChatCodeGenerationEngine getGenerationEngine(ChatGenModeEnum mode) {
        if (generationEngineMap.isEmpty()) {
            for (ChatCodeGenerationEngine engine : chatCodeGenerationEngines) {
                generationEngineMap.put(engine.mode(), engine);
            }
        }
        ChatCodeGenerationEngine selected =
                generationEngineMap.getOrDefault(mode, generationEngineMap.get(ChatGenModeEnum.CLASSIC));
        ThrowUtils.throwIf(selected == null, ErrorCode.SYSTEM_ERROR, "未找到可用的代码生成引擎");
        return selected;
    }

    private boolean isWorkflowLocked(Long appId) {
        RBucket<String> bucket = redissonClient.getBucket(String.format(APP_CHAT_MODE_LOCK_KEY, appId));
        String value = bucket.get();
        return MODE_WORKFLOW.equalsIgnoreCase(value);
    }

    private void lockWorkflowMode(Long appId) {
        RBucket<String> bucket = redissonClient.getBucket(String.format(APP_CHAT_MODE_LOCK_KEY, appId));
        bucket.set(MODE_WORKFLOW);
    }






    @Override
    public String deployApp(Long appId, User loginUser) {
        return deployApp(appId, loginUser, null);
    }

    @Override
    public String deployApp(Long appId, User loginUser, Long versionId) {
        // 1. 参数校验
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用 ID 不能为空");
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR, "用户未登录");
        // 2. 查询应用信息
        App app = this.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        // 3. 验证用户是否有权限部署该应用
        if (!app.getUserId().equals(loginUser.getId()) && !UserConstant.ADMIN_ROLE.equals(loginUser.getUserRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限部署该应用");
        }
        String appDeployUrl = appVersionService.deployVersion(app, loginUser, versionId);
        generateAppScreenshotAsync(appId, appDeployUrl);
        return appDeployUrl;

    }



    /**
     * 删除应用时关联删除对话历史
     *
     * @param id 应用ID
     * @return 是否成功
     */
    @Override
    public boolean removeById(Serializable id) {
        if (id == null) {
            return false;
        }
        // 转换为 Long 类型
        Long appId = Long.valueOf(id.toString());
        if (appId <= 0) {
            return false;
        }
        // 先查询应用，判断是否为精选应用（删除后需要清理精选缓存）
        App oldApp = this.getById(appId);
        // 先删除关联的对话历史
        try {
            chatHistoryService.deleteByAppId(appId);
        } catch (Exception e) {
            // 记录日志但不阻止应用删除
            log.error("删除应用关联对话历史失败: {}", e.getMessage());
        }
        // 再删除关联的事件日志（逻辑删除 isDelete = 1）
        try {
            chatEventLogService.deleteByAppId(appId);
        } catch (Exception e) {
            // 记录日志但不阻止应用删除
            log.error("删除应用关联事件日志失败: {}", e.getMessage());
        }
        // 删除应用
        boolean removed = super.removeById(id);
        // 应用删除成功后，清理会话模式锁，避免 Redis 残留脏数据
        if (removed) {
            try {
                RBucket<String> bucket = redissonClient.getBucket(String.format(APP_CHAT_MODE_LOCK_KEY, appId));
                bucket.delete();
            } catch (Exception e) {
                // 不影响主流程，记录日志方便排查
                log.warn("清理应用会话模式锁失败, appId={}, error={}", appId, e.getMessage());
            }
        }
        if (removed
                && oldApp != null
                && AppConstant.GOOD_APP_PRIORITY.equals(oldApp.getPriority())) {
            evictGoodAppPageCache();
        }
        return removed;
    }

    /**
     * 清理精选应用分页缓存，避免删除精选应用后首页仍命中旧数据。
     */
    private void evictGoodAppPageCache() {
        try {
            Cache cache = cacheManager.getCache("good_app_page");
            if (cache != null) {
                cache.clear();
            }
        } catch (Exception e) {
            // 不影响主流程，记录日志方便排查
            log.warn("清理精选应用缓存失败: {}", e.getMessage());
        }
    }




    /**
     * 异步生成本地应用截图并更新封面
     *
     * @param appId  应用ID
     * @param appUrl 应用访问URL
     */
    @Override
    public void generateAppScreenshotAsync(Long appId, String appUrl) {
        // 使用虚拟线程异步执行
        Thread.startVirtualThread(() -> {
            try {
                String screenshotUrl = screenshotService.generateScreenshotUrl(appUrl);
                if (StrUtil.isBlank(screenshotUrl)) {
                    log.warn("未生成应用截图，保留现有封面 appId={}", appId);
                    return;
                }
                App updateApp = new App();
                updateApp.setId(appId);
                updateApp.setCover(screenshotUrl);
                if (!this.updateById(updateApp)) {
                    log.error("截图已生成但应用封面更新失败 appId={}", appId);
                }
            } catch (Exception e) {
                log.error("应用截图生成失败，保留现有封面 appId={}", appId, e);
            }
        });
    }



    @Override
    public Long createApp(AppAddRequest appAddRequest, User loginUser) {
        // 参数校验
        String initPrompt = appAddRequest.getInitPrompt();
        ThrowUtils.throwIf(StrUtil.isBlank(initPrompt), ErrorCode.PARAMS_ERROR, "初始化 prompt 不能为空");
        // 构造入库对象
        App app = new App();
        BeanUtil.copyProperties(appAddRequest, app);
        app.setUserId(loginUser.getId());
        // 应用名称暂时为 initPrompt 前 12 位
        app.setAppName(initPrompt.substring(0, Math.min(initPrompt.length(), 12)));
        // 使用 AI 智能选择代码生成类型
        CodeGenTypeEnum selectedCodeGenType = aiCodeGenTypeRoutingService.routeCodeGenType(initPrompt);
        app.setCodeGenType(selectedCodeGenType.getValue());
        // 插入数据库
        boolean result = this.save(app);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        log.info("应用创建成功，ID: {}, 类型: {}", app.getId(), selectedCodeGenType.getValue());
        return app.getId();
    }


}
