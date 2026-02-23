package com.lunaroj.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserPublicProfileVO {

    private Long id;
    private String username;
    private String nickname;
    private String permissionGroupName;
    private String profile;
    private LocalDateTime createdAt;
}




