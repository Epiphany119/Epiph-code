package com.ai.epiphcode.service.impl;

import cn.hutool.core.util.StrUtil;
import com.ai.epiphcode.constant.AppConstant;
import com.ai.epiphcode.exception.ErrorCode;
import com.ai.epiphcode.exception.ThrowUtils;
import com.ai.epiphcode.service.ScreenshotService;
import com.ai.epiphcode.utils.WebScreenshotUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;

@Service
@Slf4j
public class ScreenshotServiceImpl implements ScreenshotService {

    @Value("${app.screenshot-base-url:http://localhost:8123/api/static}")
    private String screenshotBaseUrl;

    @Override
    public String generateScreenshotUrl(String webUrl) {
        ThrowUtils.throwIf(StrUtil.isBlank(webUrl), ErrorCode.PARAMS_ERROR, "网页URL不能为空");
        log.info("开始生成网页截图，URL: {}", webUrl);
        // 1. 生成本地截图
        String localScreenshotPath = WebScreenshotUtils.saveWebPageScreenshot(webUrl);
        ThrowUtils.throwIf(StrUtil.isBlank(localScreenshotPath), ErrorCode.OPERATION_ERROR, "本地截图生成失败");
        Path screenshotRoot = Path.of(AppConstant.SCREENSHOT_ROOT_DIR).toAbsolutePath().normalize();
        Path screenshotFile = Path.of(localScreenshotPath).toAbsolutePath().normalize();
        ThrowUtils.throwIf(!screenshotFile.startsWith(screenshotRoot), ErrorCode.SYSTEM_ERROR, "截图路径不在本地截图目录中");
        ThrowUtils.throwIf(!Files.isRegularFile(screenshotFile), ErrorCode.OPERATION_ERROR, "本地截图文件不存在");
        String relativePath = screenshotRoot.relativize(screenshotFile).toString().replace('\\', '/');
        String staticUrl = screenshotBaseUrl.replaceAll("/+$", "") + "/screenshots/" + relativePath;
        log.info("网页截图生成并保存在本地: {} -> {}", webUrl, staticUrl);
        return staticUrl;
    }
}
