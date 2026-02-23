package com.lunaroj.service.impl;

import com.lunaroj.constant.PermissionGroupNames;
import com.lunaroj.model.dto.LoginDTO;
import com.lunaroj.model.dto.RegisterDTO;
import com.lunaroj.model.vo.AuthTokenVO;
import com.lunaroj.model.entity.UserEntity;
import com.lunaroj.mapper.UserMapper;
import com.lunaroj.service.PermissionGroupService;
import com.lunaroj.security.JwtProperties;
import com.lunaroj.security.JwtTokenProvider;
import com.lunaroj.service.AuthService;
import com.lunaroj.service.CaptchaService;
import com.lunaroj.service.SystemConfigService;
import com.lunaroj.service.UserQueryService;
import com.lunaroj.common.exception.BusinessException;
import com.lunaroj.common.error.ErrorCode;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final String AUTH_BLACKLIST_KEY_PREFIX = "auth:blacklist:";
    private static final String AUTH_REFRESH_KEY_PREFIX = "auth:refresh:";
    private static final String AUTH_REFRESH_INDEX_KEY_PREFIX = "auth:refresh:index:";

    private final UserMapper userMapper;
    private final PermissionGroupService permissionGroupService;
    private final SystemConfigService systemConfigService;
    private final PasswordEncoder passwordEncoder;
    private final CaptchaService captchaService;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;
    private final StringRedisTemplate stringRedisTemplate;
    private final UserQueryService userQueryService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void register(RegisterDTO request) {
        captchaService.verifyCaptcha(request.getCaptchaId(), request.getCaptchaCode());
        if (!systemConfigService.isRegisterEnabled()) {
            throw new BusinessException(ErrorCode.REGISTER_DISABLED);
        }
        if (userQueryService.findActiveUserByUsername(request.getUsername()) != null) {
            throw new BusinessException(ErrorCode.USERNAME_EXISTS);
        }
        String email = StringUtils.hasText(request.getEmail()) ? request.getEmail().trim() : null;
        if (StringUtils.hasText(email) && userQueryService.findActiveUserByEmail(email) != null) {
            throw new BusinessException(ErrorCode.EMAIL_EXISTS);
        }

        UserEntity userEntity = new UserEntity();
        userEntity.setUsername(request.getUsername());
        userEntity.setPassword(passwordEncoder.encode(request.getPassword()));
        userEntity.setNickname(StringUtils.hasText(request.getNickname()) ? request.getNickname() : request.getUsername());
        userEntity.setEmail(email);
        userEntity.setEmailVerified(Boolean.FALSE);
        userEntity.setPermissionGroupId(permissionGroupService.getGroupIdByName(PermissionGroupNames.USER));
        userEntity.setDefaultCodePublic(Boolean.FALSE);

        try {
            userMapper.insert(userEntity);
        } catch (DuplicateKeyException ex) { // 乐观防止并发
            if (StringUtils.hasText(email) && userQueryService.findActiveUserByEmail(email) != null) {
                throw new BusinessException(ErrorCode.EMAIL_EXISTS);
            }
            throw new BusinessException(ErrorCode.USERNAME_EXISTS);
        }
    }

    @Override
    public AuthTokenVO login(LoginDTO request) {
        captchaService.verifyCaptcha(request.getCaptchaId(), request.getCaptchaCode());
        UserEntity userEntity = userQueryService.findActiveUserByUsername(request.getUsername());
        if (userEntity == null || !passwordEncoder.matches(request.getPassword(), userEntity.getPassword())) {
            throw new BusinessException(ErrorCode.PASSWORD_INCORRECT);
        }

        UserEntity updateEntity = new UserEntity();
        updateEntity.setId(userEntity.getId());
        updateEntity.setLastLoginAt(LocalDateTime.now());
        userMapper.updateById(updateEntity);

        return issueTokens(userEntity);
    }

    @Override
    public AuthTokenVO refresh(String refreshToken) {
        Claims claims = jwtTokenProvider.parseClaims(refreshToken);
        if (!jwtTokenProvider.isRefreshToken(claims)) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID);
        }

        Long userId = Long.valueOf(claims.getSubject());
        String tokenId = claims.getId();
        if (!StringUtils.hasText(tokenId)) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID);
        }
        String refreshKey = refreshSessionKey(userId, tokenId);
        String refreshSession = stringRedisTemplate.opsForValue().getAndDelete(refreshKey);
        if (!StringUtils.hasText(refreshSession)) {
            throw new BusinessException(ErrorCode.REFRESH_SESSION_INVALID);
        }
        removeRefreshTokenIndex(userId, tokenId);

        UserEntity userEntity = userQueryService.getActiveUserByIdOrThrow(userId);
        return issueTokens(userEntity);
    }

    @Override
    public void logout(String authorizationHeader, String refreshToken) {
        try {
            blacklistAccessToken(authorizationHeader);
        } catch (BusinessException ignored) {
            // Logout should stay idempotent for invalid/expired access token.
        }
        try {
            revokeRefreshToken(refreshToken);
        } catch (BusinessException ignored) {
            // Logout should stay idempotent for invalid/expired refresh token.
        }
    }

    private AuthTokenVO issueTokens(UserEntity userEntity) {
        Long userId = userEntity.getId();
        String username = userEntity.getUsername();
        String permissionGroup = permissionGroupService.getGroupNameById(userEntity.getPermissionGroupId());

        String accessToken = jwtTokenProvider.createAccessToken(userId, username, permissionGroup);
        String refreshToken = jwtTokenProvider.createRefreshToken(userId, username, permissionGroup);
        Claims refreshClaims = jwtTokenProvider.parseClaims(refreshToken);
        String refreshTokenId = refreshClaims.getId();
        if (!StringUtils.hasText(refreshTokenId)) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "刷新令牌缺少唯一标识");
        }

        String refreshKey = refreshSessionKey(userId, refreshTokenId);
        String refreshIndexKey = refreshSessionIndexKey(userId);
        Duration refreshExpire = Duration.ofSeconds(jwtProperties.getRefreshTokenExpireSeconds());
        stringRedisTemplate.opsForValue().set(
                refreshKey,
                username,
                refreshExpire
        );
        stringRedisTemplate.opsForSet().add(refreshIndexKey, refreshTokenId);
        stringRedisTemplate.expire(refreshIndexKey, refreshExpire);

        return new AuthTokenVO(
                accessToken,
                refreshToken,
                "Bearer",
                jwtProperties.getAccessTokenExpireSeconds()
        );
    }

    private String refreshSessionKey(Long userId, String tokenId) {
        return AUTH_REFRESH_KEY_PREFIX + userId + ":" + tokenId;
    }

    private String refreshSessionIndexKey(Long userId) {
        return AUTH_REFRESH_INDEX_KEY_PREFIX + userId;
    }

    private void blacklistAccessToken(String authorizationHeader) {
        if (!StringUtils.hasText(authorizationHeader) || !authorizationHeader.startsWith("Bearer ")) {
            return;
        }
        String token = authorizationHeader.substring(7);
        Claims claims = jwtTokenProvider.parseClaims(token);
        if (!jwtTokenProvider.isAccessToken(claims)) {
            return;
        }
        Date expiration = claims.getExpiration();
        if (expiration == null) {
            return;
        }
        long ttlMillis = expiration.getTime() - System.currentTimeMillis();
        if (ttlMillis <= 0) {
            return;
        }
        stringRedisTemplate.opsForValue().set(
                AUTH_BLACKLIST_KEY_PREFIX + claims.getId(),
                "1",
                Duration.ofMillis(ttlMillis)
        );
    }

    private void revokeRefreshToken(String refreshToken) {
        if (!StringUtils.hasText(refreshToken)) {
            return;
        }
        Claims claims = jwtTokenProvider.parseClaims(refreshToken);
        if (!jwtTokenProvider.isRefreshToken(claims)) {
            return;
        }
        Long userId = Long.valueOf(claims.getSubject());
        String tokenId = claims.getId();
        if (!StringUtils.hasText(tokenId)) {
            return;
        }
        String refreshKey = refreshSessionKey(userId, tokenId);
        stringRedisTemplate.delete(refreshKey);
        removeRefreshTokenIndex(userId, tokenId);
    }

    private void removeRefreshTokenIndex(Long userId, String tokenId) {
        stringRedisTemplate.opsForSet().remove(refreshSessionIndexKey(userId), tokenId);
    }
}





