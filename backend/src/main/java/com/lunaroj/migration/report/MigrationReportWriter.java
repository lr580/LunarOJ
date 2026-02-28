package com.lunaroj.migration.report;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lunaroj.migration.core.MigrationPathResolver;
import com.lunaroj.migration.module.user.UserMigrationReport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
@RequiredArgsConstructor
public class MigrationReportWriter {

    private final ObjectMapper objectMapper;
    private final MigrationPathResolver pathResolver;

    public Path writeUserReport(UserMigrationReport report) throws IOException {
        Path reportsDir = pathResolver.reportsDir();
        Files.createDirectories(reportsDir);
        Path reportPath = reportsDir.resolve(report.getJobId() + "-user-report.json");
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(reportPath.toFile(), report);
        return reportPath;
    }
}

