package com.lunaroj.service;

import com.lunaroj.model.entity.UserEntity;

public interface UserQueryService {

    UserEntity findActiveUserById(Long userId);

    UserEntity getActiveUserByIdOrThrow(Long userId);

    UserEntity findActiveUserByUsername(String username);

    UserEntity findActiveUserByEmail(String email);

    boolean existsActiveUserByEmailExcludeUserId(String email, Long excludeUserId);
}


