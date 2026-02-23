package com.lunaroj.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserProfileVO {

    private Long id;
    private String username;
    private String nickname;
    private String email;
    private Boolean emailVerified;
    private String profile;
    private String permissionGroupName;
    private Boolean defaultCodePublic;
    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;
}




