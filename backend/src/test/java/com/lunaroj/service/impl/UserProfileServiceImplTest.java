package com.lunaroj.service.impl;

import com.lunaroj.common.error.ErrorCode;
import com.lunaroj.common.exception.BusinessException;
import com.lunaroj.mapper.UserMapper;
import com.lunaroj.model.dto.ChangePasswordDTO;
import com.lunaroj.model.dto.UpdateUserBasicDTO;
import com.lunaroj.model.entity.UserEntity;
import com.lunaroj.model.vo.UserProfileVO;
import com.lunaroj.service.PermissionGroupService;
import com.lunaroj.service.UserQueryService;
import com.lunaroj.utils.converter.UserProfileConverter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceImplTest {

    @Mock
    private UserMapper userMapper;
    @Mock
    private PermissionGroupService permissionGroupService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private UserQueryService userQueryService;
    @Mock
    private UserProfileConverter userProfileConverter;
    @Mock
    private StringRedisTemplate stringRedisTemplate;
    @Mock
    private SetOperations<String, String> setOperations;

    private UserProfileServiceImpl userProfileService;

    @BeforeEach
    void setUp() {
        userProfileService = new UserProfileServiceImpl(
                userMapper,
                permissionGroupService,
                passwordEncoder,
                userQueryService,
                userProfileConverter,
                stringRedisTemplate
        );
    }

    @Test
    void getCurrentUserProfileShouldFillPermissionDisplayName() {
        UserEntity user = new UserEntity();
        user.setId(1L);
        user.setPermissionGroupId(2L);

        UserProfileVO profileVO = new UserProfileVO();
        when(userQueryService.getActiveUserByIdOrThrow(1L)).thenReturn(user);
        when(userProfileConverter.toCurrentUserProfileVO(user)).thenReturn(profileVO);
        when(permissionGroupService.getGroupDisplayNameById(2L)).thenReturn("普通用户");

        UserProfileVO result = userProfileService.getCurrentUserProfile(1L);

        assertThat(result.getPermissionGroupName()).isEqualTo("普通用户");
    }

    @Test
    void updateCurrentUserBasicShouldSkipUpdateWhenNoFieldProvided() {
        UserEntity user = new UserEntity();
        user.setId(1L);
        when(userQueryService.getActiveUserByIdOrThrow(1L)).thenReturn(user);

        UpdateUserBasicDTO request = new UpdateUserBasicDTO();
        userProfileService.updateCurrentUserBasic(1L, request);

        verify(userMapper, never()).update(any(), any());
    }

    @Test
    void updateCurrentUserBasicShouldFailWhenEmailAlreadyExists() {
        UserEntity user = new UserEntity();
        user.setId(1L);
        user.setEmail("old@example.com");
        when(userQueryService.getActiveUserByIdOrThrow(1L)).thenReturn(user);
        when(userQueryService.existsActiveUserByEmailExcludeUserId("new@example.com", 1L)).thenReturn(true);

        UpdateUserBasicDTO request = new UpdateUserBasicDTO();
        request.setEmail("new@example.com");

        assertThatThrownBy(() -> userProfileService.updateCurrentUserBasic(1L, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.EMAIL_EXISTS));

        verify(userMapper, never()).update(any(), any());
    }

    @Test
    void changePasswordShouldUpdatePasswordAndRevokeAllRefreshSessions() {
        UserEntity user = new UserEntity();
        user.setId(1L);
        user.setPassword("encoded-old");
        when(userQueryService.getActiveUserByIdOrThrow(1L)).thenReturn(user);
        when(passwordEncoder.matches("oldPass", "encoded-old")).thenReturn(true);
        when(passwordEncoder.matches("newPass", "encoded-old")).thenReturn(false);
        when(passwordEncoder.encode("newPass")).thenReturn("encoded-new");
        when(stringRedisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.members("auth:refresh:index:1")).thenReturn(new LinkedHashSet<>(Set.of("t1", "t2", " ")));

        ChangePasswordDTO request = new ChangePasswordDTO();
        request.setOldPassword("oldPass");
        request.setNewPassword("newPass");

        userProfileService.changePassword(1L, request);

        ArgumentCaptor<UserEntity> updateCaptor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userMapper).updateById(updateCaptor.capture());
        assertThat(updateCaptor.getValue().getId()).isEqualTo(1L);
        assertThat(updateCaptor.getValue().getPassword()).isEqualTo("encoded-new");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<String>> keysCaptor = ArgumentCaptor.forClass(Collection.class);
        verify(stringRedisTemplate).delete(keysCaptor.capture());
        assertThat(keysCaptor.getValue()).containsExactlyInAnyOrder(
                "auth:refresh:1:t1",
                "auth:refresh:1:t2"
        );
        verify(stringRedisTemplate).delete("auth:refresh:index:1");
    }

    @Test
    void changePasswordShouldFailWhenOldPasswordMismatch() {
        UserEntity user = new UserEntity();
        user.setId(1L);
        user.setPassword("encoded-old");
        when(userQueryService.getActiveUserByIdOrThrow(1L)).thenReturn(user);
        when(passwordEncoder.matches("oldPass", "encoded-old")).thenReturn(false);

        ChangePasswordDTO request = new ChangePasswordDTO();
        request.setOldPassword("oldPass");
        request.setNewPassword("newPass");

        assertThatThrownBy(() -> userProfileService.changePassword(1L, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.PASSWORD_INCORRECT));

        verify(userMapper, never()).updateById(any(UserEntity.class));
        verify(stringRedisTemplate, never()).delete("auth:refresh:index:1");
    }

    @Test
    void changePasswordShouldFailWhenNewPasswordSameAsOld() {
        UserEntity user = new UserEntity();
        user.setId(1L);
        user.setPassword("encoded-old");
        when(userQueryService.getActiveUserByIdOrThrow(1L)).thenReturn(user);
        when(passwordEncoder.matches("oldPass", "encoded-old")).thenReturn(true);

        ChangePasswordDTO request = new ChangePasswordDTO();
        request.setOldPassword("oldPass");
        request.setNewPassword("oldPass");

        assertThatThrownBy(() -> userProfileService.changePassword(1L, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.BAD_REQUEST));

        verify(userMapper, never()).updateById(any(UserEntity.class));
        verify(stringRedisTemplate, never()).delete("auth:refresh:index:1");
    }
}
