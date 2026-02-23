package com.lunaroj.service.impl;

import com.lunaroj.common.error.ErrorCode;
import com.lunaroj.common.exception.BusinessException;
import com.lunaroj.constant.PermissionGroupNames;
import com.lunaroj.mapper.UserMapper;
import com.lunaroj.model.dto.LoginDTO;
import com.lunaroj.model.dto.RegisterDTO;
import com.lunaroj.model.entity.UserEntity;
import com.lunaroj.model.vo.AuthTokenVO;
import com.lunaroj.security.JwtProperties;
import com.lunaroj.security.JwtTokenProvider;
import com.lunaroj.service.CaptchaService;
import com.lunaroj.service.PermissionGroupService;
import com.lunaroj.service.SystemConfigService;
import com.lunaroj.service.UserQueryService;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserMapper userMapper;
    @Mock
    private PermissionGroupService permissionGroupService;
    @Mock
    private SystemConfigService systemConfigService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private CaptchaService captchaService;
    @Mock
    private JwtTokenProvider jwtTokenProvider;
    @Mock
    private JwtProperties jwtProperties;
    @Mock
    private StringRedisTemplate stringRedisTemplate;
    @Mock
    private UserQueryService userQueryService;
    @Mock
    private ValueOperations<String, String> valueOperations;
    @Mock
    private SetOperations<String, String> setOperations;

    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(
                userMapper,
                permissionGroupService,
                systemConfigService,
                passwordEncoder,
                captchaService,
                jwtTokenProvider,
                jwtProperties,
                stringRedisTemplate,
                userQueryService
        );
    }

    @Test
    void registerShouldCreateUserWithDefaultValues() {
        RegisterDTO request = new RegisterDTO();
        request.setUsername("alice");
        request.setPassword("rawPass");
        request.setCaptchaId("cid");
        request.setCaptchaCode("code");

        when(systemConfigService.isRegisterEnabled()).thenReturn(true);
        when(userQueryService.findActiveUserByUsername("alice")).thenReturn(null);
        when(passwordEncoder.encode("rawPass")).thenReturn("encoded-pass");
        when(permissionGroupService.getGroupIdByName(PermissionGroupNames.USER)).thenReturn(2L);

        authService.register(request);

        ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userMapper).insert(captor.capture());
        UserEntity inserted = captor.getValue();
        assertThat(inserted.getUsername()).isEqualTo("alice");
        assertThat(inserted.getPassword()).isEqualTo("encoded-pass");
        assertThat(inserted.getNickname()).isEqualTo("alice");
        assertThat(inserted.getEmail()).isNull();
        assertThat(inserted.getEmailVerified()).isFalse();
        assertThat(inserted.getPermissionGroupId()).isEqualTo(2L);
        assertThat(inserted.getDefaultCodePublic()).isFalse();
    }

    @Test
    void registerShouldFailWhenRegisterDisabled() {
        RegisterDTO request = new RegisterDTO();
        request.setUsername("alice");
        request.setPassword("rawPass");
        request.setCaptchaId("cid");
        request.setCaptchaCode("code");

        when(systemConfigService.isRegisterEnabled()).thenReturn(false);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.REGISTER_DISABLED));

        verify(userMapper, never()).insert(any(UserEntity.class));
    }

    @Test
    void loginShouldIssueTokensAndPersistRefreshSession() {
        LoginDTO request = new LoginDTO();
        request.setUsername("alice");
        request.setPassword("rawPass");
        request.setCaptchaId("cid");
        request.setCaptchaCode("code");

        UserEntity user = new UserEntity();
        user.setId(1L);
        user.setUsername("alice");
        user.setPassword("encoded-pass");
        user.setPermissionGroupId(2L);

        Claims refreshClaims = org.mockito.Mockito.mock(Claims.class);
        when(refreshClaims.getId()).thenReturn("rt-001");

        when(userQueryService.findActiveUserByUsername("alice")).thenReturn(user);
        when(passwordEncoder.matches("rawPass", "encoded-pass")).thenReturn(true);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(stringRedisTemplate.opsForSet()).thenReturn(setOperations);
        when(jwtProperties.getAccessTokenExpireSeconds()).thenReturn(1800L);
        when(jwtProperties.getRefreshTokenExpireSeconds()).thenReturn(604800L);
        when(permissionGroupService.getGroupNameById(2L)).thenReturn("user");
        when(jwtTokenProvider.createAccessToken(1L, "alice", "user")).thenReturn("access-token");
        when(jwtTokenProvider.createRefreshToken(1L, "alice", "user")).thenReturn("refresh-token");
        when(jwtTokenProvider.parseClaims("refresh-token")).thenReturn(refreshClaims);

        AuthTokenVO vo = authService.login(request);

        assertThat(vo.getAccessToken()).isEqualTo("access-token");
        assertThat(vo.getRefreshToken()).isEqualTo("refresh-token");
        assertThat(vo.getTokenType()).isEqualTo("Bearer");
        assertThat(vo.getExpiresIn()).isEqualTo(1800L);

        ArgumentCaptor<UserEntity> updateCaptor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userMapper).updateById(updateCaptor.capture());
        assertThat(updateCaptor.getValue().getId()).isEqualTo(1L);
        assertThat(updateCaptor.getValue().getLastLoginAt()).isNotNull();

        verify(valueOperations).set(
                "auth:refresh:1:rt-001",
                "alice",
                Duration.ofSeconds(604800L)
        );
        verify(setOperations).add("auth:refresh:index:1", "rt-001");
        verify(stringRedisTemplate).expire("auth:refresh:index:1", Duration.ofSeconds(604800L));
    }

    @Test
    void refreshShouldRotateTokensWhenSessionExists() {
        Claims oldClaims = org.mockito.Mockito.mock(Claims.class);
        when(oldClaims.getSubject()).thenReturn("1");
        when(oldClaims.getId()).thenReturn("old-rt");

        Claims newClaims = org.mockito.Mockito.mock(Claims.class);
        when(newClaims.getId()).thenReturn("new-rt");

        UserEntity user = new UserEntity();
        user.setId(1L);
        user.setUsername("alice");
        user.setPermissionGroupId(2L);

        when(jwtTokenProvider.parseClaims("old-refresh")).thenReturn(oldClaims);
        when(jwtTokenProvider.isRefreshToken(oldClaims)).thenReturn(true);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(stringRedisTemplate.opsForSet()).thenReturn(setOperations);
        when(jwtProperties.getAccessTokenExpireSeconds()).thenReturn(1800L);
        when(jwtProperties.getRefreshTokenExpireSeconds()).thenReturn(604800L);
        when(valueOperations.getAndDelete("auth:refresh:1:old-rt")).thenReturn("alice");
        when(userQueryService.getActiveUserByIdOrThrow(1L)).thenReturn(user);
        when(permissionGroupService.getGroupNameById(2L)).thenReturn("user");
        when(jwtTokenProvider.createAccessToken(1L, "alice", "user")).thenReturn("new-access");
        when(jwtTokenProvider.createRefreshToken(1L, "alice", "user")).thenReturn("new-refresh");
        when(jwtTokenProvider.parseClaims("new-refresh")).thenReturn(newClaims);

        AuthTokenVO vo = authService.refresh("old-refresh");

        assertThat(vo.getAccessToken()).isEqualTo("new-access");
        assertThat(vo.getRefreshToken()).isEqualTo("new-refresh");
        verify(setOperations).remove("auth:refresh:index:1", "old-rt");
        verify(valueOperations).set("auth:refresh:1:new-rt", "alice", Duration.ofSeconds(604800L));
        verify(setOperations).add("auth:refresh:index:1", "new-rt");
    }

    @Test
    void refreshShouldFailWhenRefreshSessionMissing() {
        Claims oldClaims = org.mockito.Mockito.mock(Claims.class);
        when(oldClaims.getSubject()).thenReturn("1");
        when(oldClaims.getId()).thenReturn("old-rt");

        when(jwtTokenProvider.parseClaims("old-refresh")).thenReturn(oldClaims);
        when(jwtTokenProvider.isRefreshToken(oldClaims)).thenReturn(true);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.getAndDelete("auth:refresh:1:old-rt")).thenReturn(null);

        assertThatThrownBy(() -> authService.refresh("old-refresh"))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.REFRESH_SESSION_INVALID));

        verify(userQueryService, never()).getActiveUserByIdOrThrow(any());
    }

    @Test
    void logoutShouldBeIdempotentWhenTokenParsingFails() {
        doThrow(new BusinessException(ErrorCode.TOKEN_INVALID))
                .when(jwtTokenProvider).parseClaims(eq("bad-access"));
        doThrow(new BusinessException(ErrorCode.TOKEN_INVALID))
                .when(jwtTokenProvider).parseClaims(eq("bad-refresh"));

        authService.logout("Bearer bad-access", "bad-refresh");

        verify(jwtTokenProvider).parseClaims("bad-access");
        verify(jwtTokenProvider).parseClaims("bad-refresh");
    }
}
