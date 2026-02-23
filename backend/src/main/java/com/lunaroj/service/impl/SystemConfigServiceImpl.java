package com.lunaroj.service.impl;

import com.lunaroj.model.entity.SystemConfigEntity;
import com.lunaroj.mapper.SystemConfigMapper;
import com.lunaroj.service.SystemConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class SystemConfigServiceImpl implements SystemConfigService {

    private static final String CONFIG_REGISTER_ENABLED = "register_enabled";

    private final SystemConfigMapper systemConfigMapper;

    @Override
    public String getString(String configKey, String defaultValue) {
        SystemConfigEntity entity = systemConfigMapper.selectById(configKey);
        if (entity == null || !StringUtils.hasText(entity.getConfigValue())) {
            return defaultValue;
        }
        return entity.getConfigValue();
    }

    @Override
    public boolean getBoolean(String configKey, boolean defaultValue) {
        String configValue = getString(configKey, null);
        if (!StringUtils.hasText(configValue)) {
            return defaultValue;
        }
        return Boolean.parseBoolean(configValue);
    }

    @Override
    public long getLong(String configKey, long defaultValue) {
        String configValue = getString(configKey, null);
        if (!StringUtils.hasText(configValue)) {
            return defaultValue;
        }
        try {
            return Long.parseLong(configValue);
        } catch (NumberFormatException ex) {
            log.warn("无效的 long 配置值: key={}, value={}, 使用默认值={}", configKey, configValue, defaultValue);
            return defaultValue;
        }
    }

    @Override
    public boolean isRegisterEnabled() {
        return getBoolean(CONFIG_REGISTER_ENABLED, true);
    }
}


