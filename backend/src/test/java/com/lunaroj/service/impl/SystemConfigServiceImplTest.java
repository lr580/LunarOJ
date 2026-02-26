package com.lunaroj.service.impl;

import com.lunaroj.mapper.SystemConfigMapper;
import com.lunaroj.model.entity.SystemConfigEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SystemConfigServiceImplTest {

    @Mock
    private SystemConfigMapper systemConfigMapper;

    private SystemConfigServiceImpl systemConfigService;

    @BeforeEach
    void setUp() {
        systemConfigService = new SystemConfigServiceImpl(systemConfigMapper);
    }

    @Test
    void getStringShouldReturnDefaultWhenConfigMissing() {
        when(systemConfigMapper.selectById("missing_key")).thenReturn(null);

        String value = systemConfigService.getString("missing_key", "default-value");

        assertThat(value).isEqualTo("default-value");
    }

    @Test
    void getStringShouldReturnStoredValueWhenPresent() {
        SystemConfigEntity entity = new SystemConfigEntity();
        entity.setConfigKey("site_name");
        entity.setConfigValue("LunarOJ");
        when(systemConfigMapper.selectById("site_name")).thenReturn(entity);

        String value = systemConfigService.getString("site_name", "default-value");

        assertThat(value).isEqualTo("LunarOJ");
    }

    @Test
    void getBooleanShouldFallbackToDefaultWhenValueBlank() {
        SystemConfigEntity entity = new SystemConfigEntity();
        entity.setConfigKey("feature_x");
        entity.setConfigValue("  ");
        when(systemConfigMapper.selectById("feature_x")).thenReturn(entity);

        boolean enabled = systemConfigService.getBoolean("feature_x", true);

        assertThat(enabled).isTrue();
    }

    @Test
    void getBooleanShouldParseConfiguredValue() {
        SystemConfigEntity entity = new SystemConfigEntity();
        entity.setConfigKey("feature_x");
        entity.setConfigValue("false");
        when(systemConfigMapper.selectById("feature_x")).thenReturn(entity);

        boolean enabled = systemConfigService.getBoolean("feature_x", true);

        assertThat(enabled).isFalse();
    }

    @Test
    void getBooleanShouldParseCaseInsensitiveTrue() {
        SystemConfigEntity entity = new SystemConfigEntity();
        entity.setConfigKey("feature_x");
        entity.setConfigValue("TRUE");
        when(systemConfigMapper.selectById("feature_x")).thenReturn(entity);

        boolean enabled = systemConfigService.getBoolean("feature_x", false);

        assertThat(enabled).isTrue();
    }

    @Test
    void getLongShouldFallbackToDefaultWhenNumberInvalid() {
        SystemConfigEntity entity = new SystemConfigEntity();
        entity.setConfigKey("limit");
        entity.setConfigValue("not_a_number");
        when(systemConfigMapper.selectById("limit")).thenReturn(entity);

        long value = systemConfigService.getLong("limit", 1024L);

        assertThat(value).isEqualTo(1024L);
    }

    @Test
    void getLongShouldReturnConfiguredValueWhenNumberValid() {
        SystemConfigEntity entity = new SystemConfigEntity();
        entity.setConfigKey("limit");
        entity.setConfigValue("2048");
        when(systemConfigMapper.selectById("limit")).thenReturn(entity);

        long value = systemConfigService.getLong("limit", 1024L);

        assertThat(value).isEqualTo(2048L);
    }

    @Test
    void isRegisterEnabledShouldReadBooleanConfig() {
        SystemConfigEntity entity = new SystemConfigEntity();
        entity.setConfigKey("register_enabled");
        entity.setConfigValue("false");
        when(systemConfigMapper.selectById("register_enabled")).thenReturn(entity);

        boolean enabled = systemConfigService.isRegisterEnabled();

        assertThat(enabled).isFalse();
    }
}
