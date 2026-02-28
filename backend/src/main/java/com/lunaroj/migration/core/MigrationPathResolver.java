package com.lunaroj.migration.core;

import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Component
public class MigrationPathResolver {

    public Path baseDir() {
        Path cwd = Paths.get("").toAbsolutePath().normalize();
        Path repoRoot = resolveRepoRoot(cwd);
        return repoRoot.resolve("temp").resolve("migration");
    }

    public Path profilesDir() {
        return baseDir().resolve("profiles");
    }

    public Path reportsDir() {
        return baseDir().resolve("reports");
    }

    private Path resolveRepoRoot(Path cwd) {
        if (Files.isDirectory(cwd.resolve("backend")) && Files.isDirectory(cwd.resolve("docs"))) {
            return cwd;
        }
        Path fileName = cwd.getFileName();
        if (fileName != null && "backend".equals(fileName.toString()) && cwd.getParent() != null) {
            return cwd.getParent();
        }
        return cwd;
    }
}

