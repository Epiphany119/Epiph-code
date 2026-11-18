package com.ai.epiphcode.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.json.JSONUtil;
import com.ai.epiphcode.constant.AppConstant;
import com.ai.epiphcode.constant.UserConstant;
import com.ai.epiphcode.core.builder.VueProjectBuilder;
import com.ai.epiphcode.core.handler.TurnAccumulator;
import com.ai.epiphcode.exception.BusinessException;
import com.ai.epiphcode.exception.ErrorCode;
import com.ai.epiphcode.mapper.AppDeploymentHistoryMapper;
import com.ai.epiphcode.mapper.AppMapper;
import com.ai.epiphcode.mapper.AppVersionMapper;
import com.ai.epiphcode.model.entity.App;
import com.ai.epiphcode.model.entity.AppDeploymentHistory;
import com.ai.epiphcode.model.entity.AppVersion;
import com.ai.epiphcode.model.entity.User;
import com.ai.epiphcode.model.vo.AppVO;
import com.ai.epiphcode.model.vo.AppVersionCompareVO;
import com.ai.epiphcode.model.vo.AppVersionFileChangeVO;
import com.ai.epiphcode.model.vo.AppVersionVO;
import com.ai.epiphcode.model.vo.AppDeploymentHistoryVO;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.stream.Stream;

/** Immutable snapshots and deployment history for generated applications. */
@Service
@Slf4j
public class AppVersionService extends ServiceImpl<AppVersionMapper, AppVersion> {

    private static final String GENERATED = "GENERATED";
    private static final String LEGACY_BASELINE = "LEGACY_BASELINE";
    private static final String LEGACY_DEPLOYED = "LEGACY_DEPLOYED";
    private static final int MAX_DIFF_LINES = 1200;
    private static final long MAX_DIFF_BYTES = 256 * 1024;

    @Resource
    private AppMapper appMapper;
    @Resource
    private AppDeploymentHistoryMapper deploymentHistoryMapper;
    @Resource
    private VueProjectBuilder vueProjectBuilder;

    @Value("${app.deploy-base-url:http://localhost:8123/api/static}")
    private String deployBaseUrl;

    private final Map<Long, Semaphore> appLocks = new ConcurrentHashMap<>();

    public List<AppVersionVO> listVersions(App app, User user) {
        assertCanManage(app, user);
        Semaphore lock = lockFor(app.getId());
        lock.acquireUninterruptibly();
        try {
            App freshApp = appMapper.selectOneById(app.getId());
            ensureLegacyBaseline(freshApp);
            App latestApp = appMapper.selectOneById(app.getId());
            return versionsFor(app.getId()).stream().map(version -> toVO(version, latestApp)).toList();
        } finally {
            lock.release();
        }
    }

    public List<AppDeploymentHistoryVO> listDeploymentHistory(App app, User user) {
        assertCanManage(app, user);
        return deploymentHistoryMapper.selectListByQuery(QueryWrapper.create()
                        .eq(AppDeploymentHistory::getAppId, app.getId())
                        .orderBy(AppDeploymentHistory::getDeployedTime, false))
                .stream().map(history -> {
                    AppVersion version = findVersion(app.getId(), history.getVersionId());
                    AppDeploymentHistoryVO vo = new AppDeploymentHistoryVO();
                    vo.setId(String.valueOf(history.getId()));
                    vo.setVersionId(String.valueOf(history.getVersionId()));
                    vo.setVersionNo(version == null ? null : version.getVersionNo());
                    vo.setDeployKey(history.getDeployKey());
                    vo.setDeployedBy(String.valueOf(history.getDeployedBy()));
                    vo.setDeployedTime(history.getDeployedTime());
                    return vo;
                }).toList();
    }

    /** Adds the displayed version numbers to application cards with one batch lookup. */
    public void fillVersionNumbers(List<App> apps, List<AppVO> appVOs) {
        if (apps == null || appVOs == null || apps.isEmpty() || appVOs.isEmpty()) return;
        Set<Long> versionIds = apps.stream()
                .flatMap(app -> Stream.of(app.getWorkingVersionId(), app.getDeployedVersionId()))
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());
        if (versionIds.isEmpty()) return;
        Map<Long, Integer> versionNumbers = this.list(QueryWrapper.create()
                        .in(AppVersion::getId, versionIds.toArray(Long[]::new)))
                .stream().collect(java.util.stream.Collectors.toMap(AppVersion::getId, AppVersion::getVersionNo));
        int count = Math.min(apps.size(), appVOs.size());
        for (int i = 0; i < count; i++) {
            App app = apps.get(i);
            AppVO appVO = appVOs.get(i);
            appVO.setWorkingVersionNo(versionNumbers.get(app.getWorkingVersionId()));
            appVO.setDeployedVersionNo(versionNumbers.get(app.getDeployedVersionId()));
        }
    }

    /** Locks one app's mutable worktree until its stream flushes, and restores the chosen base. */
    public Long beginTurn(App app, User user, Long requestedBaseVersionId) {
        assertCanManage(app, user);
        Semaphore lock = lockFor(app.getId());
        lock.acquireUninterruptibly();
        try {
            App freshApp = appMapper.selectOneById(app.getId());
            if (freshApp == null) throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "应用不存在");
            ensureLegacyBaseline(freshApp);
            freshApp = appMapper.selectOneById(app.getId());
            Long baseId = requestedBaseVersionId == null ? freshApp.getWorkingVersionId() : requestedBaseVersionId;
            AppVersion base = baseId == null ? null : findVersion(app.getId(), baseId);
            if (baseId != null && base == null) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "所选修改基线不存在或不属于当前应用");
            }
            if (base != null) restoreToWorkspace(base);
            return baseId;
        } catch (RuntimeException e) {
            lock.release();
            throw e;
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public AppVersion commitTurn(TurnAccumulator accumulator) {
        try {
            Path workspace = codeWorkspace(accumulator.getCodeGenType(), accumulator.getAppId());
            if (!hasFiles(workspace)) return null;
            App app = appMapper.selectOneById(accumulator.getAppId());
            if (app == null) throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "应用不存在");
            AppVersion parent = accumulator.getBaseVersionId() == null
                    ? null : findVersion(accumulator.getAppId(), accumulator.getBaseVersionId());
            AppVersion version = createSnapshot(app, workspace, accumulator.getUserId(), GENERATED,
                    accumulator.getTurnId(), accumulator.getUserMessage(), accumulator.getAgent(),
                    parent == null ? null : parent.getId());
            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCompletion(int status) {
                        if (status == STATUS_ROLLED_BACK) {
                            try { rollbackTurn(accumulator); }
                            catch (RuntimeException e) { log.error("数据库回滚后恢复网页工作区失败 turnId={}", accumulator.getTurnId(), e); }
                        }
                    }
                });
            }
            App update = new App();
            update.setId(app.getId());
            update.setWorkingVersionId(version.getId());
            if (appMapper.update(update) <= 0) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "保存工作版本指针失败");
            }
            accumulator.setCreatedVersion(version.getId(), version.getVersionNo());
            return version;
        } catch (RuntimeException e) {
            rollbackTurn(accumulator);
            throw e;
        }
    }

    public void rollbackTurn(TurnAccumulator accumulator) {
        if (accumulator.getBaseVersionId() == null) {
            try { deleteTree(codeWorkspace(accumulator.getCodeGenType(), accumulator.getAppId())); }
            catch (IOException e) { log.warn("清理失败轮次的临时代码失败 appId={}", accumulator.getAppId(), e); }
            return;
        }
        AppVersion base = findVersion(accumulator.getAppId(), accumulator.getBaseVersionId());
        if (base != null) restoreToWorkspace(base);
    }

    public void finishTurn(TurnAccumulator accumulator) {
        Semaphore lock = appLocks.get(accumulator.getAppId());
        if (lock == null || !accumulator.releaseVersionLockOnce()) return;
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    lock.release();
                }
            });
        } else {
            lock.release();
        }
    }

    public Path authorizedPreviewRoot(Long versionId, User user) {
        AppVersion version = getById(versionId);
        if (version == null) throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "版本不存在");
        App app = appMapper.selectOneById(version.getAppId());
        boolean isPubliclyDeployed = app != null && version.getId().equals(app.getDeployedVersionId());
        if (!isPubliclyDeployed) assertCanManage(app, user);
        Path root = snapshotDir(version).toAbsolutePath().normalize();
        if ("vue_project".equals(version.getCodeGenType()) && Files.isDirectory(root.resolve("dist"))) {
            return root.resolve("dist");
        }
        return root;
    }

    public AppVersionCompareVO compare(App app, User user, Long leftVersionId, Long rightVersionId) {
        assertCanManage(app, user);
        AppVersion left = findVersion(app.getId(), leftVersionId);
        AppVersion right = findVersion(app.getId(), rightVersionId);
        if (left == null || right == null) throw new BusinessException(ErrorCode.PARAMS_ERROR, "比较版本无效");
        Map<String, FileInfo> oldFiles;
        Map<String, FileInfo> newFiles;
        try {
            oldFiles = fileIndex(snapshotDir(left));
            newFiles = fileIndex(snapshotDir(right));
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "读取版本文件失败：" + e.getMessage());
        }
        List<String> paths = new ArrayList<>();
        paths.addAll(oldFiles.keySet());
        for (String path : newFiles.keySet()) if (!oldFiles.containsKey(path)) paths.add(path);
        paths.sort(String::compareTo);
        List<AppVersionFileChangeVO> changes = new ArrayList<>();
        for (String path : paths) {
            FileInfo oldInfo = oldFiles.get(path);
            FileInfo newInfo = newFiles.get(path);
            String status = oldInfo == null ? "ADDED" : newInfo == null ? "DELETED"
                    : oldInfo.sha256().equals(newInfo.sha256()) ? "UNCHANGED" : "MODIFIED";
            if ("UNCHANGED".equals(status)) continue;
            AppVersionFileChangeVO file = new AppVersionFileChangeVO();
            file.setPath(path);
            file.setStatus(status);
            file.setOldBytes(oldInfo == null ? null : oldInfo.bytes());
            file.setNewBytes(newInfo == null ? null : newInfo.bytes());
            if ("ADDED".equals(status) || "DELETED".equals(status) || "MODIFIED".equals(status)) {
                file.setUnifiedDiff(unifiedDiff(path, oldInfo, newInfo,
                        left == null ? null : snapshotDir(left), right == null ? null : snapshotDir(right)));
            }
            changes.add(file);
        }
        AppVersionCompareVO result = new AppVersionCompareVO();
        result.setLeftVersionId(String.valueOf(leftVersionId));
        result.setRightVersionId(String.valueOf(rightVersionId));
        result.setFiles(changes);
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public String deployVersion(App app, User user, Long requestedVersionId) {
        assertCanManage(app, user);
        Semaphore lock = lockFor(app.getId());
        lock.acquireUninterruptibly();
        Path liveDir = null;
        Path stagedDir = null;
        Path backupDir = null;
        Path temporaryBuildDir = null;
        boolean movedOld = false;
        boolean installedNew = false;
        try {
            App freshApp = appMapper.selectOneById(app.getId());
            ensureLegacyBaseline(freshApp);
            freshApp = appMapper.selectOneById(app.getId());
            Long versionId = requestedVersionId == null ? freshApp.getWorkingVersionId() : requestedVersionId;
            AppVersion version = versionId == null ? null : findVersion(app.getId(), versionId);
            if (version == null) throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "没有可部署的网页版本");

            Path publishSource = snapshotDir(version);
            if ("vue_project".equals(version.getCodeGenType())) {
                if (Files.isDirectory(publishSource.resolve("dist"))) {
                    publishSource = publishSource.resolve("dist");
                } else if (Files.isRegularFile(publishSource.resolve("package.json"))) {
                    temporaryBuildDir = Path.of(AppConstant.STORAGE_ROOT_DIR, "version_build", String.valueOf(version.getId()));
                    deleteTree(temporaryBuildDir);
                    copyTree(snapshotDir(version), temporaryBuildDir, true);
                    if (!vueProjectBuilder.buildProject(temporaryBuildDir.toString()) || !Files.isDirectory(temporaryBuildDir.resolve("dist"))) {
                        throw new BusinessException(ErrorCode.SYSTEM_ERROR, "所选 Vue 版本构建失败");
                    }
                    publishSource = temporaryBuildDir.resolve("dist");
                }
            }

            String deployKey = freshApp.getDeployKey();
            if (deployKey == null || deployKey.isBlank()) deployKey = IdUtil.simpleUUID().substring(0, 6);
            Path deployRoot = Path.of(AppConstant.CODE_DEPLOY_ROOT_DIR).toAbsolutePath().normalize();
            Files.createDirectories(deployRoot);
            liveDir = deployRoot.resolve(deployKey).normalize();
            if (!liveDir.startsWith(deployRoot)) throw new BusinessException(ErrorCode.PARAMS_ERROR, "部署目录无效");
            stagedDir = deployRoot.resolve("." + deployKey + ".staging-" + version.getId());
            backupDir = deployRoot.resolve("." + deployKey + ".backup-" + System.nanoTime());
            deleteTree(stagedDir);
            copyTree(publishSource, stagedDir, true);
            if (!hasFiles(stagedDir)) throw new BusinessException(ErrorCode.SYSTEM_ERROR, "所选版本没有可发布的网页文件");
            if (!fileIndex(publishSource).equals(fileIndex(stagedDir))) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "部署文件校验失败，线上版本未切换");
            }
            if (Files.exists(liveDir)) {
                movePath(liveDir, backupDir);
                movedOld = true;
            }
            try {
                movePath(stagedDir, liveDir);
                installedNew = true;
            } catch (IOException e) {
                if (movedOld && Files.exists(backupDir)) movePath(backupDir, liveDir);
                throw e;
            }

            LocalDateTime now = LocalDateTime.now();
            App update = new App();
            update.setId(freshApp.getId());
            update.setDeployKey(deployKey);
            update.setDeployedTime(now);
            update.setDeployedVersionId(version.getId());
            if (appMapper.update(update) <= 0) throw new BusinessException(ErrorCode.OPERATION_ERROR, "更新已部署版本失败");
            AppDeploymentHistory history = AppDeploymentHistory.builder()
                    .appId(freshApp.getId())
                    .versionId(version.getId())
                    .deployKey(deployKey)
                    .deployedBy(user.getId())
                    .deployedTime(now)
                    .build();
            if (deploymentHistoryMapper.insert(history) <= 0) throw new BusinessException(ErrorCode.OPERATION_ERROR, "保存部署记录失败");
            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                Path publishedLive = liveDir;
                Path previousBackup = backupDir;
                boolean hadPrevious = movedOld;
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        if (hadPrevious) {
                            try { deleteTree(previousBackup); }
                            catch (IOException e) { log.warn("部署成功但旧目录备份清理失败: {}", previousBackup, e); }
                        }
                    }

                    @Override
                    public void afterCompletion(int status) {
                        if (status == STATUS_ROLLED_BACK) {
                            restoreDeployment(publishedLive, previousBackup, null, hadPrevious, true);
                        }
                    }
                });
            } else if (movedOld) {
                deleteTree(backupDir);
            }
            return deployBaseUrl.replaceAll("/+$", "") + "/" + deployKey + "/";
        } catch (BusinessException e) {
            restoreDeployment(liveDir, backupDir, stagedDir, movedOld, installedNew);
            throw e;
        } catch (Exception e) {
            restoreDeployment(liveDir, backupDir, stagedDir, movedOld, installedNew);
            log.error("部署版本失败 appId={}, versionId={}", app.getId(), requestedVersionId, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "部署失败：" + e.getMessage());
        } finally {
            if (temporaryBuildDir != null) {
                try { deleteTree(temporaryBuildDir); }
                catch (IOException e) { log.warn("清理 Vue 版本临时构建目录失败: {}", temporaryBuildDir, e); }
            }
            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCompletion(int status) {
                        lock.release();
                    }
                });
            } else {
                lock.release();
            }
        }
    }

    private void ensureLegacyBaseline(App app) {
        if (app == null) return;
        List<AppVersion> existing = versionsFor(app.getId());
        if (!existing.isEmpty()) {
            AppVersion working = existing.getFirst();
            AppVersion deployed = null;
            if (app.getDeployedVersionId() == null && app.getDeployKey() != null && !app.getDeployKey().isBlank()) {
                Path deployedDir = Path.of(AppConstant.CODE_DEPLOY_ROOT_DIR, app.getDeployKey());
                if (hasFiles(deployedDir)) {
                    deployed = existing.stream().filter(version -> sameWebFiles(version, deployedDir)).findFirst().orElse(null);
                    if (deployed == null) {
                        deployed = createSnapshot(app, deployedDir, app.getUserId(), LEGACY_DEPLOYED,
                                null, "历史部署基线（上线前的线上代码）", null, null);
                    }
                }
            }
            if (app.getWorkingVersionId() == null || (deployed != null && app.getDeployedVersionId() == null)) {
                App update = new App();
                update.setId(app.getId());
                if (app.getWorkingVersionId() == null) update.setWorkingVersionId(working.getId());
                if (deployed != null) update.setDeployedVersionId(deployed.getId());
                appMapper.update(update);
            }
            return;
        }
        Path current = codeWorkspace(app.getCodeGenType(), app.getId());
        AppVersion working = null;
        if (hasFiles(current)) {
            working = createSnapshot(app, current, app.getUserId(), LEGACY_BASELINE,
                    null, "历史基线（上线前的当前代码）", null, null);
        }
        AppVersion deployed = null;
        if (app.getDeployKey() != null && !app.getDeployKey().isBlank()) {
            Path deployedDir = Path.of(AppConstant.CODE_DEPLOY_ROOT_DIR, app.getDeployKey());
            if (hasFiles(deployedDir)) {
                if (working != null && sameWebFiles(working, deployedDir)) {
                    deployed = working;
                } else {
                    deployed = createSnapshot(app, deployedDir, app.getUserId(), LEGACY_DEPLOYED,
                            null, "历史部署基线（上线前的线上代码）", null, null);
                }
            }
        }
        if (working != null || deployed != null) {
            App update = new App();
            update.setId(app.getId());
            update.setWorkingVersionId(working != null ? working.getId() : deployed.getId());
            if (deployed != null) update.setDeployedVersionId(deployed.getId());
            appMapper.update(update);
        }
    }

    private AppVersion createSnapshot(App app, Path source, Long creatorId, String type,
                                     String turnId, String prompt, String agent, Long parentId) {
        try {
            Map<String, FileInfo> index = fileIndex(source);
            if (index.isEmpty()) throw new BusinessException(ErrorCode.SYSTEM_ERROR, "没有可保存的网页文件");
            int versionNo = versionsFor(app.getId()).stream().map(AppVersion::getVersionNo)
                    .filter(value -> value != null).max(Integer::compareTo).orElse(0) + 1;
            long id = Long.parseLong(IdUtil.getSnowflakeNextIdStr());
            Path finalDir = Path.of(AppConstant.CODE_VERSION_ROOT_DIR, String.valueOf(app.getId()), String.valueOf(id));
            Path staging = finalDir.resolveSibling(".staging-" + id);
            deleteTree(staging);
            Files.createDirectories(finalDir.getParent());
            copyTree(source, staging, true);
            movePath(staging, finalDir);
            long totalBytes = index.values().stream().mapToLong(FileInfo::bytes).sum();
            Map<String, String> hashes = new LinkedHashMap<>();
            index.forEach((path, info) -> hashes.put(path, info.sha256()));
            AppVersion version = AppVersion.builder()
                    .id(id)
                    .appId(app.getId())
                    .versionNo(versionNo)
                    .parentVersionId(parentId)
                    .turnId(turnId)
                    .userPrompt(prompt)
                    .agent(agent)
                    .codeGenType(app.getCodeGenType())
                    .versionType(type)
                    .fileManifest(JSONUtil.toJsonStr(hashes))
                    .totalBytes(totalBytes)
                    .createUserId(creatorId)
                    .createTime(LocalDateTime.now())
                    .build();
            if (!this.save(version)) {
                deleteTree(finalDir);
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "保存版本信息失败");
            }
            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                Path snapshotToCleanup = finalDir;
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCompletion(int status) {
                        if (status == STATUS_ROLLED_BACK) {
                            try { deleteTree(snapshotToCleanup); }
                            catch (IOException e) { log.warn("回滚版本元数据后清理快照失败: {}", snapshotToCleanup, e); }
                        }
                    }
                });
            }
            return version;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "保存网页版本失败：" + e.getMessage());
        }
    }

    private void restoreToWorkspace(AppVersion version) {
        Path target = codeWorkspace(version.getCodeGenType(), version.getAppId());
        try {
            Files.createDirectories(target);
            try (Stream<Path> children = Files.list(target)) {
                for (Path child : children.toList()) {
                    if ("vue_project".equals(version.getCodeGenType()) && "node_modules".equals(child.getFileName().toString())) continue;
                    deleteTree(child);
                }
            }
            copyTree(snapshotDir(version), target, true);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "恢复修改基线失败：" + e.getMessage());
        }
    }

    private List<AppVersion> versionsFor(Long appId) {
        return this.list(QueryWrapper.create().eq(AppVersion::getAppId, appId)
                .orderBy(AppVersion::getVersionNo, false));
    }

    private AppVersion findVersion(Long appId, Long versionId) {
        if (appId == null || versionId == null) return null;
        List<AppVersion> matches = this.list(QueryWrapper.create()
                .eq(AppVersion::getAppId, appId)
                .eq(AppVersion::getId, versionId)
                .limit(0, 1));
        return matches.isEmpty() ? null : matches.getFirst();
    }

    private AppVersionVO toVO(AppVersion version, App app) {
        AppVersionVO vo = new AppVersionVO();
        vo.setId(String.valueOf(version.getId()));
        vo.setAppId(String.valueOf(version.getAppId()));
        vo.setVersionNo(version.getVersionNo());
        vo.setParentVersionId(version.getParentVersionId() == null ? null : String.valueOf(version.getParentVersionId()));
        vo.setTurnId(version.getTurnId());
        vo.setUserPrompt(version.getUserPrompt());
        vo.setAgent(version.getAgent());
        vo.setCodeGenType(version.getCodeGenType());
        vo.setVersionType(version.getVersionType());
        vo.setTotalBytes(version.getTotalBytes());
        vo.setCreateUserId(version.getCreateUserId() == null ? null : String.valueOf(version.getCreateUserId()));
        vo.setCreateTime(version.getCreateTime());
        vo.setWorking(version.getId().equals(app.getWorkingVersionId()));
        vo.setDeployed(version.getId().equals(app.getDeployedVersionId()));
        return vo;
    }

    private void assertCanManage(App app, User user) {
        if (app == null) throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        if (user == null || (!UserConstant.ADMIN_ROLE.equals(user.getUserRole()) && !app.getUserId().equals(user.getId()))) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权访问该应用的网页版本");
        }
    }

    private Path codeWorkspace(String codeGenType, Long appId) {
        return Path.of(AppConstant.CODE_OUTPUT_ROOT_DIR, codeGenType + "_" + appId).toAbsolutePath().normalize();
    }

    private Path snapshotDir(AppVersion version) {
        Path root = Path.of(AppConstant.CODE_VERSION_ROOT_DIR).toAbsolutePath().normalize();
        Path path = root.resolve(String.valueOf(version.getAppId())).resolve(String.valueOf(version.getId())).normalize();
        if (!path.startsWith(root)) throw new BusinessException(ErrorCode.PARAMS_ERROR, "版本路径无效");
        return path;
    }

    private Semaphore lockFor(Long appId) {
        return appLocks.computeIfAbsent(appId, ignored -> new Semaphore(1));
    }

    private boolean sameWebFiles(AppVersion version, Path deployedDir) {
        try {
            Path compareRoot = "vue_project".equals(version.getCodeGenType()) && Files.isDirectory(snapshotDir(version).resolve("dist"))
                    ? snapshotDir(version).resolve("dist") : snapshotDir(version);
            Map<String, FileInfo> left = fileIndex(compareRoot);
            Map<String, FileInfo> right = fileIndex(deployedDir);
            if (!left.keySet().equals(right.keySet())) return false;
            return left.keySet().stream().allMatch(path -> left.get(path).sha256().equals(right.get(path).sha256()));
        } catch (Exception e) {
            return false;
        }
    }

    private Map<String, FileInfo> fileIndex(Path root) throws IOException {
        Map<String, FileInfo> result = new LinkedHashMap<>();
        if (!Files.isDirectory(root)) return result;
        try (Stream<Path> paths = Files.walk(root)) {
            for (Path file : paths.filter(Files::isRegularFile).filter(path -> !Files.isSymbolicLink(path)).sorted().toList()) {
                Path relative = root.relativize(file);
                if (shouldSkip(relative)) continue;
                result.put(relative.toString().replace('\\', '/'), new FileInfo(Files.size(file), sha256(file)));
            }
        }
        return result;
    }

    private boolean shouldSkip(Path relative) {
        for (Path part : relative) {
            String name = part.toString();
            if ("node_modules".equals(name) || ".git".equals(name) || ".DS_Store".equals(name)) return true;
        }
        return false;
    }

    private boolean hasFiles(Path root) {
        try { return !fileIndex(root).isEmpty(); }
        catch (IOException e) { return false; }
    }

    private String sha256(Path file) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (var input = Files.newInputStream(file)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = input.read(buffer)) >= 0) digest.update(buffer, 0, read);
            }
            return java.util.HexFormat.of().formatHex(digest.digest());
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private void copyTree(Path source, Path target, boolean skipDependencies) throws IOException {
        if (!Files.isDirectory(source)) throw new IOException("源目录不存在：" + source);
        Files.createDirectories(target);
        try (Stream<Path> paths = Files.walk(source)) {
            for (Path item : paths.toList()) {
                Path relative = source.relativize(item);
                if (relative.toString().isEmpty()) continue;
                if (skipDependencies && shouldSkip(relative)) continue;
                Path destination = target.resolve(relative.toString()).normalize();
                if (!destination.startsWith(target.normalize())) throw new IOException("发现越界文件路径");
                if (Files.isSymbolicLink(item)) continue;
                if (Files.isDirectory(item)) Files.createDirectories(destination);
                else if (Files.isRegularFile(item)) {
                    Files.createDirectories(destination.getParent());
                    Files.copy(item, destination, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
                }
            }
        }
    }

    private void deleteTree(Path root) throws IOException {
        if (root == null || !Files.exists(root)) return;
        try (Stream<Path> paths = Files.walk(root)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
        }
    }

    private void movePath(Path source, Path target) throws IOException {
        try { Files.move(source, target, StandardCopyOption.ATOMIC_MOVE); }
        catch (IOException e) { Files.move(source, target); }
    }

    private void restoreDeployment(Path live, Path backup, Path staging, boolean movedOld, boolean installedNew) {
        try {
            if (staging != null) deleteTree(staging);
            if (live != null && installedNew) deleteTree(live);
            if (live != null && movedOld && backup != null && Files.exists(backup)) {
                if (Files.exists(live)) deleteTree(live);
                movePath(backup, live);
            }
        } catch (Exception e) {
            log.error("部署失败后的目录回滚也失败，备份目录={}", backup, e);
        }
    }

    private String unifiedDiff(String path, FileInfo oldInfo, FileInfo newInfo, Path oldRoot, Path newRoot) {
        try {
            if ((oldInfo != null && oldInfo.bytes() > MAX_DIFF_BYTES) || (newInfo != null && newInfo.bytes() > MAX_DIFF_BYTES)) {
                return "文件较大，已省略逐行差异";
            }
            byte[] oldBytes = oldInfo == null ? new byte[0] : Files.readAllBytes(oldRoot.resolve(path));
            byte[] newBytes = newInfo == null ? new byte[0] : Files.readAllBytes(newRoot.resolve(path));
            if (containsBinary(oldBytes) || containsBinary(newBytes)) return "二进制文件，仅显示文件状态和大小";
            String[] oldLines = new String(oldBytes, java.nio.charset.StandardCharsets.UTF_8).split("\\R", -1);
            String[] newLines = new String(newBytes, java.nio.charset.StandardCharsets.UTF_8).split("\\R", -1);
            if (oldLines.length > MAX_DIFF_LINES || newLines.length > MAX_DIFF_LINES) return "文本行数较多，已省略逐行差异";
            int[][] lcs = new int[oldLines.length + 1][newLines.length + 1];
            for (int i = oldLines.length - 1; i >= 0; i--) {
                for (int j = newLines.length - 1; j >= 0; j--) {
                    lcs[i][j] = oldLines[i].equals(newLines[j]) ? lcs[i + 1][j + 1] + 1
                            : Math.max(lcs[i + 1][j], lcs[i][j + 1]);
                }
            }
            StringBuilder diff = new StringBuilder("--- ").append(oldInfo == null ? "/dev/null" : "V-old/" + path)
                    .append("\n+++ ").append(newInfo == null ? "/dev/null" : "V-new/" + path)
                    .append("\n@@ -1,").append(oldLines.length).append(" +1,").append(newLines.length).append(" @@\n");
            int i = 0, j = 0;
            while (i < oldLines.length || j < newLines.length) {
                if (i < oldLines.length && j < newLines.length && oldLines[i].equals(newLines[j])) {
                    diff.append(' ').append(oldLines[i++]).append('\n');
                    j++;
                } else if (i < oldLines.length && (j == newLines.length || lcs[i + 1][j] >= lcs[i][j + 1])) {
                    diff.append('-').append(oldLines[i++]).append('\n');
                } else {
                    diff.append('+').append(newLines[j++]).append('\n');
                }
                if (diff.length() > 24000) return diff.substring(0, 24000) + "\n…差异内容过长，已截断";
            }
            return diff.toString();
        } catch (Exception ignored) {
            return "无法读取文本差异";
        }
    }

    private boolean containsBinary(byte[] bytes) {
        for (byte value : bytes) if (value == 0) return true;
        return false;
    }

    private record FileInfo(long bytes, String sha256) { }
}
