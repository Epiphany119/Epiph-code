package com.ai.epiphcode.service;

/**
 * 截图服务
 */
public interface ScreenshotService {


    /**
     * 生成截图并返回本地静态资源地址
     *
     * @param webUrl 网址
     * @return 本地静态资源 URL
     */
    String generateScreenshotUrl(String webUrl);

}
