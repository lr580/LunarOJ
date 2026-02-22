package com.lunaroj.auth.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lunaroj.auth.converter.UserProfileConverter;
import com.lunaroj.auth.dto.req.ChangePasswordRequest;
import com.lunaroj.auth.dto.req.UpdateUserBasicRequest;
import com.lunaroj.auth.dto.req.UpdateUserProfileRequest;
import com.lunaroj.auth.dto.resp.UserPublicProfileResponse;
import com.lunaroj.auth.dto.resp.UserProfileResponse;
import com.lunaroj.auth.entity.UserEntity;
import com.lunaroj.auth.mapper.UserMapper;
import com.lunaroj.auth.service.PermissionGroupService;
import com.lunaroj.auth.service.UserQueryService;
import com.lunaroj.auth.service.UserProfileService;
import com.lunaroj.common.BusinessException;
import com.lunaroj.common.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserProfileServiceImpl implements UserProfileService {

    private static final String AUTH_REFRESH_KEY_PREFIX = "auth:refresh:";

    private final UserMapper userMapper;
    private final PermissionGroupService permissionGroupService;
    private final PasswordEncoder passwordEncoder;
    private final UserQueryService userQueryService;
    private final UserProfileConverter userProfileConverter;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public UserProfileResponse getCurrentUserProfile(Long userId) {
        UserEntity userEntity = userQueryService.getActiveUserByIdOrThrow(userId);
        return userProfileConverter.toCurrentUserProfileResponse(userEntity, permissionGroupService);
    }

    @Override
    public UserPublicProfileResponse getUserPublicProfileByUsername(String username) {
        if (!StringUtils.hasText(username)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "用户名不能为空");
        }
        UserEntity userEntity = userQueryService.findActiveUserByUsername(username.trim());
        if (userEntity == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        return userProfileConverter.toPublicProfileResponse(userEntity, permissionGroupService);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCurrentUserBasic(Long userId, UpdateUserBasicRequest request) {
        UserEntity currentUser = userQueryService.getActiveUserByIdOrThrow(userId);
        var updateWrapper = Wrappers.<UserEntity>lambdaUpdate()
                .eq(UserEntity::getId, userId)
                .isNull(UserEntity::getDeletedAt);

        boolean hasUpdate = false;

        if (request.getNickname() != null) {
            String nickname = request.getNickname().trim();
            if (!StringUtils.hasText(nickname)) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "昵称不能为空");
            }
            updateWrapper.set(UserEntity::getNickname, nickname);
            hasUpdate = true;
        }

        if (request.getEmail() != null) {
            String email = request.getEmail().trim();
            if (!StringUtils.hasText(email)) {
                updateWrapper.set(UserEntity::getEmail, null);
                updateWrapper.set(UserEntity::getEmailVerified, Boolean.FALSE);
                hasUpdate = true;
            } else {
                if (!email.equals(currentUser.getEmail())
                        && userQueryService.existsActiveUserByEmailExcludeUserId(email, userId)) {
                    throw new BusinessException(ErrorCode.EMAIL_EXISTS);
                }
                updateWrapper.set(UserEntity::getEmail, email);
                if (!email.equals(currentUser.getEmail())) {
                    updateWrapper.set(UserEntity::getEmailVerified, Boolean.FALSE);
                }
                hasUpdate = true;
            }
        }

        if (request.getDefaultCodePublic() != null) {
            updateWrapper.set(UserEntity::getDefaultCodePublic, request.getDefaultCodePublic());
            hasUpdate = true;
        }

        if (!hasUpdate) {
            return;
        }
        try {
            userMapper.update(null, updateWrapper);
        } catch (DuplicateKeyException ex) {
            throw new BusinessException(ErrorCode.EMAIL_EXISTS);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCurrentUserProfile(Long userId, UpdateUserProfileRequest request) {
        userQueryService.getActiveUserByIdOrThrow(userId);
        UserEntity updateEntity = new UserEntity();
        updateEntity.setId(userId);
        updateEntity.setProfile(request.getProfile());
        userMapper.updateById(updateEntity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changePassword(Long userId, ChangePasswordRequest request) {
        UserEntity userEntity = userQueryService.getActiveUserByIdOrThrow(userId);
        if (!passwordEncoder.matches(request.getOldPassword(), userEntity.getPassword())) {
            throw new BusinessException(ErrorCode.PASSWORD_INCORRECT, "旧密码错误");
        }
        if (passwordEncoder.matches(request.getNewPassword(), userEntity.getPassword())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "新密码不能与旧密码相同");
        }

        UserEntity updateEntity = new UserEntity();
        updateEntity.setId(userId);
        updateEntity.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userMapper.updateById(updateEntity);

        revokeAllRefreshSessions(userId);
    }

    private void revokeAllRefreshSessions(Long userId) {
        Set<String> refreshKeys = stringRedisTemplate.keys(AUTH_REFRESH_KEY_PREFIX + userId + ":*");
        if (refreshKeys == null || refreshKeys.isEmpty()) {
            return;
        }
        stringRedisTemplate.delete(refreshKeys);
    }

}
