package com.lunaroj.auth.service;

import com.lunaroj.auth.entity.UserEntity;

public interface UserQueryService {

    UserEntity findActiveUserById(Long userId);

    UserEntity getActiveUserByIdOrThrow(Long userId);

    UserEntity findActiveUserByUsername(String username);

    UserEntity findActiveUserByEmail(String email);

    boolean existsActiveUserByEmailExcludeUserId(String email, Long excludeUserId);
}
