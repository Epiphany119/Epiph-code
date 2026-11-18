package com.ai.epiphcode.constant;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.stream.Stream;

/** Resolves all generated artifacts to one stable workspace tmp directory. */
@Slf4j
public final class AppStoragePaths {

    private AppStoragePaths() {
    }

    public static String resolveStorageRoot() {
        String configured = System.getProperty("epiphcode.storage-root");
        if (configured == null || configured.isBlank()) {
            configured = System.getenv("EPIPH_CODE_STORAGE_ROOT");
        }
        Path workspaceRoot = discoverWorkspaceRoot(Path.of(System.getProperty("user.dir")));
        Path root = configured == null || configured.isBlank()
                ? workspaceRoot.resolve("tmp")
                : Path.of(configured).toAbsolutePath().normalize();
        try {
            Files.createDirectories(root);
            migrateLegacyModuleTmp(root, workspaceRoot);
        } catch (IOException e) {
            throw new IllegalStateException("无法初始化统一的应用产物目录: " + root, e);
        }
        return root.toAbsolutePath().normalize().toString();
    }

    private static Path discoverWorkspaceRoot(Path start) {
        Path current = start.toAbsolutePath().normalize();
        while (current != null) {
            if (Files.exists(current.resolve(".git"))
                    && Files.isDirectory(current.resolve("epiph-code-monolith"))) {
                return current;
            }
            current = current.getParent();
        }
        return start.toAbsolutePath().normalize();
    }

    /** Merges the old module-local directory without overwriting different files. */
    private static void migrateLegacyModuleTmp(Path canonicalTmp, Path workspaceRoot) throws IOException {
        Path legacyTmp = workspaceRoot.resolve("epiph-code-monolith").resolve("tmp");
        if (!Files.isDirectory(legacyTmp) || legacyTmp.toAbsolutePath().normalize().equals(canonicalTmp.toAbsolutePath().normalize())) {
            return;
        }
        try (Stream<Path> entries = Files.list(legacyTmp)) {
            for (Path entry : entries.toList()) {
                merge(entry, canonicalTmp.resolve(entry.getFileName()));
            }
        }
        try (Stream<Path> remaining = Files.walk(legacyTmp)) {
            for (Path path : remaining.sorted(Comparator.reverseOrder()).toList()) {
                if (Files.isDirectory(path)) Files.deleteIfExists(path);
            }
        }
        log.info("已将旧模块 tmp 产物并入统一目录: {} -> {}", legacyTmp, canonicalTmp);
    }

    private static void merge(Path source, Path target) throws IOException {
        if (Files.isDirectory(source)) {
            Files.createDirectories(target);
            try (Stream<Path> children = Files.list(source)) {
                for (Path child : children.toList()) merge(child, target.resolve(child.getFileName()));
            }
            Files.deleteIfExists(source);
            return;
        }
        Files.createDirectories(target.getParent());
        if (!Files.exists(target)) {
            try {
                Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException atomicMoveUnavailable) {
                Files.move(source, target);
            }
            return;
        }
        if (Files.mismatch(source, target) == -1) {
            Files.delete(source);
            return;
        }
        String name = source.getFileName().toString();
        Path conflict = target.resolveSibling(name + ".legacy-monolith");
        int suffix = 2;
        while (Files.exists(conflict)) {
            conflict = target.resolveSibling(name + ".legacy-monolith-" + suffix++);
        }
        Files.move(source, conflict);
        log.warn("旧 tmp 文件与规范目录同名但内容不同，已保留为: {}", conflict);
    }
}
