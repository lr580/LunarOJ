package com.lunaroj.migration.cli;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lunaroj.migration.core.MigrationPathResolver;
import com.lunaroj.migration.model.MigrationProfile;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

@Component
@RequiredArgsConstructor
public class MigrationProfileStore {

    private final ObjectMapper objectMapper;
    private final MigrationPathResolver pathResolver;

    public Path saveProfile(MigrationProfile profile, String fileName) throws IOException {
        Path profilesDir = pathResolver.profilesDir();
        Files.createDirectories(profilesDir);
        String normalizedName = normalizeFileName(fileName);
        Path path = profilesDir.resolve(normalizedName);
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), profile);
        return path;
    }

    public List<Path> listProfiles() throws IOException {
        Path profilesDir = pathResolver.profilesDir();
        if (!Files.exists(profilesDir)) {
            return List.of();
        }
        try (Stream<Path> stream = Files.list(profilesDir)) {
            return stream
                    .filter(path -> path.getFileName().toString().endsWith(".json"))
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .toList();
        }
    }

    public MigrationProfile loadProfile(Path path) throws IOException {
        return objectMapper.readValue(path.toFile(), MigrationProfile.class);
    }

    private String normalizeFileName(String fileName) {
        String normalized = fileName.replaceAll("[^A-Za-z0-9._-]", "_");
        if (!normalized.endsWith(".json")) {
            normalized += ".json";
        }
        return normalized;
    }
}

