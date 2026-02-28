package com.lunaroj.migration.model;

import lombok.Data;

@Data
public class ExecutionConfig {

    private Integer batchSize = 500;

    private Boolean dryRun = Boolean.FALSE;

    private Boolean failFast = Boolean.FALSE;

    private Integer issueLimit = 200;
}

