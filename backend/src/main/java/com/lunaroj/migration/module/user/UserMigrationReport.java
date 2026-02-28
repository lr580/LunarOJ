package com.lunaroj.migration.module.user;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class UserMigrationReport {

    private String jobId;

    private boolean dryRun;

    private LocalDateTime startedAt;

    private LocalDateTime finishedAt;

    private long sourceTotal;

    private long processed;

    private long inserted;

    private long updated;

    private long skipped;

    private long failed;

    private boolean issueLimitReached;

    private List<UserMigrationIssue> issues = new ArrayList<>();
}

