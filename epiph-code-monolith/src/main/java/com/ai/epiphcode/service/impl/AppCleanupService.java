package com.ai.epiphcode.service.impl;

import cn.hutool.core.util.StrUtil;
import com.ai.epiphcode.constant.AppConstant;
import com.ai.epiphcode.mapper.AppMapper;
import com.ai.epiphcode.model.entity.App;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * 定时清理超过保留期（默认 30 天）的已删除应用数据库记录及本地 tmp 产物。
 */
@Service
@Slf4j
public class AppCleanupService {

    private static final String SCREENSHOT_URL_PREFIX = "/api/static/screenshots/";

    @Resource
    private AppMapper appMapper;

    @Value("${epiphcode.cleanup.retention-days:30}")
    private int retentionDays;

    @EventListener(ApplicationReadyEvent.class)
    public void cleanupOnStartup() {
        purgeExpiredDeletedApps();
    }

    /**
     * 每天凌晨 3 点执行一次过期数据与产物清理
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void scheduledPurge() {
        purgeExpiredDeletedApps();
    }

    public void purgeExpiredDeletedApps() {
        int days = Math.max(1, retentionDays);
        LocalDateTime cutoffTime = LocalDateTime.now().minusDays(days);
        List<App> expiredApps = appMapper.selectExpiredDeletedApps(cutoffTime);
        if (expiredApps == null || expiredApps.isEmpty()) {
            return;
        }
        log.info("发现 {} 个超过 {} 天保留期的已删除应用，开始清理数据库与本地产物", expiredApps.size(), days);
        for (App app : expiredApps) {
            try {
                purgeSingleApp(app);
            } catch (Exception e) {
                log.error("清理过期已删除应用失败, appId={}", app.getId(), e);
            }
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void purgeSingleApp(App app) {
        if (app == null || app.getId() == null) {
            return;
        }
        Long appId = app.getId();
        // 1. 清理本地 tmp 目录产物（生成目录、版本快照、部署目录、封面截图）
        deleteAppArtifacts(app);

        // 2. 物理删除数据库关联记录与主记录
        appMapper.physicalDeleteChatHistoryByAppId(appId);
        appMapper.physicalDeleteChatEventLogByAppId(appId);
        appMapper.physicalDeleteAppVersionsByAppId(appId);
        appMapper.physicalDeleteDeploymentHistoryByAppId(appId);
        appMapper.physicalDeleteAppById(appId);
        log.info("已完成过期应用彻底清理: appId={}, deployKey={}", appId, app.getDeployKey());
    }

    private void deleteAppArtifacts(App app) {
        Long appId = app.getId();
        String appIdStr = String.valueOf(appId);

        // code_output/{codeGenType}_{appId} 及任何以 _{appId} 结尾的生成目录
        Path outputRoot = Paths.get(AppConstant.CODE_OUTPUT_ROOT_DIR).toAbsolutePath().normalize();
        if (Files.isDirectory(outputRoot)) {
            String suffix = "_" + appIdStr;
            try (Stream<Path> entries = Files.list(outputRoot)) {
                for (Path entry : entries.toList()) {
                    if (entry.getFileName().toString().endsWith(suffix)) {
                        deleteRecursively(entry);
                    }
                }
            } catch (IOException e) {
                log.warn("扫描 code_output 目录失败, appId={}, error={}", appId, e.getMessage());
            }
        }

        // code_versions/{appId}
        Path versionDir = Paths.get(AppConstant.CODE_VERSION_ROOT_DIR, appIdStr).toAbsolutePath().normalize();
        deleteRecursively(versionDir);

        // code_deploy/{deployKey}
        if (StrUtil.isNotBlank(app.getDeployKey())) {
            Path deployDir = Paths.get(AppConstant.CODE_DEPLOY_ROOT_DIR, app.getDeployKey()).toAbsolutePath().normalize();
            deleteRecursively(deployDir);
        }

        // screenshots 封面图
        deleteCoverScreenshot(app.getCover());
    }

    private void deleteCoverScreenshot(String cover) {
        if (StrUtil.isBlank(cover)) {
            return;
        }
        int idx = cover.indexOf(SCREENSHOT_URL_PREFIX);
        if (idx < 0) {
            return;
        }
        String relativePath = cover.substring(idx + SCREENSHOT_URL_PREFIX.length());
        int queryIdx = relativePath.indexOf('?');
        if (queryIdx >= 0) {
            relativePath = relativePath.substring(0, queryIdx);
        }
        if (relativePath.isBlank()) {
            return;
        }
        Path screenshotRoot = Paths.get(AppConstant.SCREENSHOT_ROOT_DIR).toAbsolutePath().normalize();
        Path target = screenshotRoot.resolve(relativePath).normalize();
        if (target.startsWith(screenshotRoot) && Files.exists(target)) {
            deleteRecursively(target);
        }
    }

    private void deleteRecursively(Path target) {
        if (target == null || !Files.exists(target)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(target)) {
            for (Path path : walk.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        } catch (IOException e) {
            log.warn("删除本地产物失败: {}, error={}", target, e.getMessage());
        }
    }
}
