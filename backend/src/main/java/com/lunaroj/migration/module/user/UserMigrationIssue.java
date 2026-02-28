package com.lunaroj.migration.module.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserMigrationIssue {

    private Long sourceUserId;

    private String username;

    private String action;

    private String reason;
}

