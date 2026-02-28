package com.lunaroj.migration.module.user;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ScnuojUserRecord {

    private Long id;

    private String username;

    private String nickname;

    private String email;

    private Integer role;

    private Integer status;

    private Integer isVerifyEmail;

    private String personalIntro;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}

