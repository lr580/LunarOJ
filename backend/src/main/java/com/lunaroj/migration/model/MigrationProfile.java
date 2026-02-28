package com.lunaroj.migration.model;

import lombok.Data;

@Data
public class MigrationProfile {

    private SourceDatabaseConfig source = new SourceDatabaseConfig();

    private ExecutionConfig execution = new ExecutionConfig();

    private ModulesConfig modules = new ModulesConfig();
}

