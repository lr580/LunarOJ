package com.lunaroj.migration.cli;

import com.lunaroj.LunarOjApplication;
import org.springframework.boot.SpringApplication;

import java.util.HashMap;
import java.util.Map;

public class MigrationCliApplication {

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(LunarOjApplication.class);
        Map<String, Object> defaults = new HashMap<>();
        defaults.put("lunaroj.migration.cli.enabled", "true");
        defaults.put("spring.main.web-application-type", "none");
        application.setDefaultProperties(defaults);
        application.run(args);
    }
}

