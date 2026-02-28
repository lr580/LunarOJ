package com.lunaroj.migration.model;

import com.lunaroj.migration.module.user.UserModuleConfig;
import lombok.Data;

@Data
public class ModulesConfig {

    private UserModuleConfig user = new UserModuleConfig();
}

