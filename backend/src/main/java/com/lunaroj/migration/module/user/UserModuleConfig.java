package com.lunaroj.migration.module.user;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class UserModuleConfig {

    private Boolean enabled = Boolean.TRUE;

    private List<Integer> statusFilter = new ArrayList<>(List.of(10));

    private UserPasswordMode passwordMode = UserPasswordMode.RANDOM_UNLOGIN;

    private String fixedPassword;

    private UsernameConflictPolicy conflictPolicy = UsernameConflictPolicy.SKIP;

    private EmailConflictPolicy emailConflictPolicy = EmailConflictPolicy.NULLIFY;

    private Boolean importProfile = Boolean.FALSE;
}

