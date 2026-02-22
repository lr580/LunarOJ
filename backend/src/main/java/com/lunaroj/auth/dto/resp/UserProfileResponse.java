package com.lunaroj.auth.dto.resp;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserProfileResponse {

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
