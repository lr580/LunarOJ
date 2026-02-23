package com.lunaroj.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lunaroj.model.entity.UserEntity;
import com.lunaroj.mapper.UserMapper;
import com.lunaroj.service.UserQueryService;
import com.lunaroj.common.exception.BusinessException;
import com.lunaroj.common.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class UserQueryServiceImpl implements UserQueryService {

    private final UserMapper userMapper;

    @Override
    public UserEntity findActiveUserById(Long userId) {
        if (userId == null) {
            return null;
        }
        UserEntity userEntity = userMapper.selectById(userId);
        if (userEntity == null || userEntity.getDeletedAt() != null) {
            return null;
        }
        return userEntity;
    }

    @Override
    public UserEntity getActiveUserByIdOrThrow(Long userId) {
        UserEntity userEntity = findActiveUserById(userId);
        if (userEntity == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        return userEntity;
    }

    @Override
    public UserEntity findActiveUserByUsername(String username) {
        if (!StringUtils.hasText(username)) {
            return null;
        }
        return userMapper.selectOne(
                Wrappers.<UserEntity>lambdaQuery()
                        .eq(UserEntity::getUsername, username)
                        .isNull(UserEntity::getDeletedAt)
                        .last("LIMIT 1")
        );
    }

    @Override
    public UserEntity findActiveUserByEmail(String email) {
        if (!StringUtils.hasText(email)) {
            return null;
        }
        return userMapper.selectOne(
                Wrappers.<UserEntity>lambdaQuery()
                        .eq(UserEntity::getEmail, email)
                        .isNull(UserEntity::getDeletedAt)
                        .last("LIMIT 1")
        );
    }

    @Override
    public boolean existsActiveUserByEmailExcludeUserId(String email, Long excludeUserId) {
        if (!StringUtils.hasText(email)) {
            return false;
        }
        Long count = userMapper.selectCount(
                Wrappers.<UserEntity>lambdaQuery()
                        .eq(UserEntity::getEmail, email)
                        .ne(excludeUserId != null, UserEntity::getId, excludeUserId)
                        .isNull(UserEntity::getDeletedAt)
        );
        return count != null && count > 0;
    }
}



