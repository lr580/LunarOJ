package com.lunaroj.auth.dto.resp;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserPublicProfileResponse {

    private Long id;
    private String username;
    private String nickname;
    private String permissionGroupName;
    private String profile;
    private LocalDateTime createdAt;
}
