package com.lunaroj.service;

public interface SystemConfigService {

    String getString(String configKey, String defaultValue);

    boolean getBoolean(String configKey, boolean defaultValue);

    long getLong(String configKey, long defaultValue);

    boolean isRegisterEnabled();
}

