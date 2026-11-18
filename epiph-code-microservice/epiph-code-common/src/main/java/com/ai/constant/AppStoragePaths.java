package com.ai.constant;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.stream.Stream;

/** Resolves microservice artifacts to the same workspace tmp directory as the monolith. */
public final class AppStoragePaths {

    private AppStoragePaths() {
    }

    public static String resolveStorageRoot() {
        String configured = System.getProperty("epiphcode.storage-root");
        if (configured == null || configured.isBlank()) configured = System.getenv("EPIPH_CODE_STORAGE_ROOT");
        Path start = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
        Path current = start;
        while (current != null && !(Files.exists(current.resolve(".git"))
                && Files.isDirectory(current.resolve("epiph-code-monolith")))) {
            current = current.getParent();
        }
        Path root = configured == null || configured.isBlank()
                ? (current == null ? start.resolve("tmp") : current.resolve("tmp"))
                : Path.of(configured).toAbsolutePath().normalize();
        try {
            Files.createDirectories(root);
            if (current != null) mergeLegacyTmp(current.resolve("epiph-code-monolith/tmp"), root);
        } catch (IOException e) {
            throw new IllegalStateException("无法初始化统一的应用产物目录: " + root, e);
        }
        return root.toAbsolutePath().normalize().toString();
    }

    private static void mergeLegacyTmp(Path legacy, Path canonical) throws IOException {
        if (!Files.isDirectory(legacy) || legacy.toAbsolutePath().normalize().equals(canonical.toAbsolutePath().normalize())) return;
        try (Stream<Path> entries = Files.list(legacy)) {
            for (Path entry : entries.toList()) merge(entry, canonical.resolve(entry.getFileName()));
        }
        try (Stream<Path> paths = Files.walk(legacy)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                if (Files.isDirectory(path)) Files.deleteIfExists(path);
            }
        }
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
            try { Files.move(source, target, StandardCopyOption.ATOMIC_MOVE); }
            catch (IOException e) { Files.move(source, target); }
            return;
        }
        if (Files.mismatch(source, target) == -1) {
            Files.delete(source);
            return;
        }
        Path conflict = target.resolveSibling(source.getFileName() + ".legacy-monolith");
        int suffix = 2;
        while (Files.exists(conflict)) conflict = target.resolveSibling(source.getFileName() + ".legacy-monolith-" + suffix++);
        Files.move(source, conflict);
    }
}
