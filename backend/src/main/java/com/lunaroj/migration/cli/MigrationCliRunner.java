package com.lunaroj.migration.cli;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "lunaroj.migration.cli", name = "enabled", havingValue = "true")
public class MigrationCliRunner implements ApplicationRunner {

    private final MigrationCliService migrationCliService;
    private final ApplicationContext applicationContext;

    @Override
    public void run(ApplicationArguments args) {
        migrationCliService.launch();
        int code = org.springframework.boot.SpringApplication.exit(applicationContext, () -> 0);
        System.exit(code);
    }
}

