package com.ai.epiphcode.controller;

import com.ai.epiphcode.constant.AppConstant;
import com.ai.epiphcode.model.entity.User;
import com.ai.epiphcode.service.UserService;
import com.ai.epiphcode.service.impl.AppVersionService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.HandlerMapping;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 静态资源访问
 */
@RestController
@RequestMapping("/static")
public class StaticResourceController {

    @Resource
    private AppVersionService appVersionService;
    @Resource
    private UserService userService;

    // 生成预览目录，以及部署后复制的目录
    private static final Path PREVIEW_ROOT_DIR = Paths.get(AppConstant.CODE_OUTPUT_ROOT_DIR).toAbsolutePath().normalize();
    private static final Path DEPLOY_ROOT_DIR = Paths.get(AppConstant.CODE_DEPLOY_ROOT_DIR).toAbsolutePath().normalize();
    private static final Path SCREENSHOT_ROOT_DIR = Paths.get(AppConstant.SCREENSHOT_ROOT_DIR).toAbsolutePath().normalize();

    @GetMapping("/version/{versionId}/**")
    public ResponseEntity<org.springframework.core.io.Resource> serveVersionPreview(@PathVariable Long versionId, HttpServletRequest request) {
        try {
            User user = null;
            try {
                user = userService.getLoginUser(request);
            } catch (Exception ignored) {
                // Published snapshots are public; unpublished snapshots still require owner/admin access.
            }
            Path rootDir = appVersionService.authorizedPreviewRoot(versionId, user).toAbsolutePath().normalize();
            String resourcePath = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
            String prefix = "/static/version/" + versionId;
            resourcePath = resourcePath.substring(prefix.length());
            if (resourcePath.isEmpty() || "/".equals(resourcePath)) resourcePath = "index.html";
            else if (resourcePath.startsWith("/")) resourcePath = resourcePath.substring(1);
            Path filePath = rootDir.resolve(resourcePath).normalize();
            if (!filePath.startsWith(rootDir) || !Files.isRegularFile(filePath)) return ResponseEntity.notFound().build();
            return ResponseEntity.ok()
                    .header("Content-Type", getContentTypeWithCharset(filePath.toString()))
                    .header(HttpHeaders.CACHE_CONTROL, "no-store")
                    .body(new FileSystemResource(filePath));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    /** Serves locally persisted application cover screenshots. */
    @GetMapping("/screenshots/**")
    public ResponseEntity<org.springframework.core.io.Resource> serveScreenshot(HttpServletRequest request) {
        try {
            String resourcePath = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
            String prefix = "/static/screenshots/";
            if (resourcePath == null || !resourcePath.startsWith(prefix)) return ResponseEntity.badRequest().build();
            Path filePath = SCREENSHOT_ROOT_DIR.resolve(resourcePath.substring(prefix.length())).normalize();
            if (!filePath.startsWith(SCREENSHOT_ROOT_DIR) || !Files.isRegularFile(filePath)) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok()
                    .header("Content-Type", getContentTypeWithCharset(filePath.toString()))
                    .header(HttpHeaders.CACHE_CONTROL, "public, max-age=3600")
                    .body(new FileSystemResource(filePath));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 提供静态资源访问，支持目录重定向
     * 访问格式：http://localhost:8123/api/static/{deployKey}[/{fileName}]
     */
    @GetMapping("/{deployKey}/**")
    public ResponseEntity<org.springframework.core.io.Resource> serveStaticResource(
            @PathVariable String deployKey,
            HttpServletRequest request) {
        try {
            if (!deployKey.matches("[A-Za-z0-9_-]+")) {
                return ResponseEntity.badRequest().build();
            }
            // 获取资源路径
            String resourcePath = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
            resourcePath = resourcePath.substring(("/static/" + deployKey).length());
            // 如果是目录访问（不带斜杠），重定向到带斜杠的URL
            if (resourcePath.isEmpty()) {
                HttpHeaders headers = new HttpHeaders();
                headers.add("Location", request.getRequestURI() + "/");
                return new ResponseEntity<>(headers, HttpStatus.MOVED_PERMANENTLY);
            }
            // 默认返回 index.html
            if (resourcePath.equals("/")) {
                resourcePath = "index.html";
            } else if (resourcePath.startsWith("/")) {
                resourcePath = resourcePath.substring(1);
            }
            // 部署目录优先；不存在时回退到生成目录，继续支持控制台预览
            Path deployDir = DEPLOY_ROOT_DIR.resolve(deployKey).normalize();
            Path previewDir = PREVIEW_ROOT_DIR.resolve(deployKey).normalize();
            Path rootDir = Files.isDirectory(deployDir) ? deployDir : previewDir;
            Path filePath = rootDir.resolve(resourcePath).normalize();
            if (!filePath.startsWith(rootDir)) {
                return ResponseEntity.badRequest().build();
            }
            File file = filePath.toFile();
            // 检查文件是否存在
            if (!file.exists()) {
                return ResponseEntity.notFound().build();
            }
            // 返回文件资源
            org.springframework.core.io.Resource resource = new FileSystemResource(file);
            return ResponseEntity.ok()
                    .header("Content-Type", getContentTypeWithCharset(filePath.toString()))
                    .header(HttpHeaders.CACHE_CONTROL, "no-cache, must-revalidate")
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 根据文件扩展名返回带字符编码的 Content-Type
     */
    private String getContentTypeWithCharset(String filePath) {
        if (filePath.endsWith(".html")) return "text/html; charset=UTF-8";
        if (filePath.endsWith(".css")) return "text/css; charset=UTF-8";
        if (filePath.endsWith(".js")) return "application/javascript; charset=UTF-8";
        if (filePath.endsWith(".json")) return "application/json; charset=UTF-8";
        if (filePath.endsWith(".svg")) return "image/svg+xml";
        if (filePath.endsWith(".png")) return "image/png";
        if (filePath.endsWith(".jpg")) return "image/jpeg";
        if (filePath.endsWith(".jpeg")) return "image/jpeg";
        if (filePath.endsWith(".webp")) return "image/webp";
        if (filePath.endsWith(".gif")) return "image/gif";
        if (filePath.endsWith(".ico")) return "image/x-icon";
        if (filePath.endsWith(".woff2")) return "font/woff2";
        if (filePath.endsWith(".woff")) return "font/woff";
        if (filePath.endsWith(".ttf")) return "font/ttf";
        return "application/octet-stream";
    }
}
