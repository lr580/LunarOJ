package com.lunaroj.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Data
@Component
@ConfigurationProperties(prefix = "lunaroj.init.root")
public class RootAccountInitProperties {

    private static final String DEFAULT_USERNAME = "root";
    private static final String DEFAULT_PASSWORD = "dynamicCD+ACAM";

    private boolean enabled = false;
    private String username = "root";
    private String password = "dynamicCD+ACAM";

    public String resolveUsername() {
        return StringUtils.hasText(username) ? username.trim() : DEFAULT_USERNAME;
    }

    public String resolvePassword() {
        return StringUtils.hasText(password) ? password.trim() : DEFAULT_PASSWORD;
    }
}

