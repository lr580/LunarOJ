package com.lunaroj.service.impl;

import com.lunaroj.common.error.ErrorCode;
import com.lunaroj.common.exception.BusinessException;
import com.lunaroj.mapper.UserMapper;
import com.lunaroj.model.entity.UserEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserQueryServiceImplTest {

    @Mock
    private UserMapper userMapper;

    private UserQueryServiceImpl userQueryService;

    @BeforeEach
    void setUp() {
        userQueryService = new UserQueryServiceImpl(userMapper);
    }

    @Test
    void findActiveUserByIdShouldReturnNullWhenUserIdNull() {
        assertThat(userQueryService.findActiveUserById(null)).isNull();
        verify(userMapper, never()).selectById(any());
    }

    @Test
    void findActiveUserByIdShouldReturnNullWhenUserSoftDeleted() {
        UserEntity user = new UserEntity();
        user.setId(1L);
        user.setDeletedAt(LocalDateTime.now());
        when(userMapper.selectById(1L)).thenReturn(user);

        assertThat(userQueryService.findActiveUserById(1L)).isNull();
    }

    @Test
    void getActiveUserByIdOrThrowShouldThrowWhenUserMissing() {
        when(userMapper.selectById(1L)).thenReturn(null);

        assertThatThrownBy(() -> userQueryService.getActiveUserByIdOrThrow(1L))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND));
    }

    @Test
    void getActiveUserByIdOrThrowShouldReturnUserWhenFound() {
        UserEntity user = new UserEntity();
        user.setId(1L);
        when(userMapper.selectById(1L)).thenReturn(user);

        UserEntity found = userQueryService.getActiveUserByIdOrThrow(1L);

        assertThat(found).isNotNull();
        assertThat(found.getId()).isEqualTo(1L);
    }

    @Test
    void findActiveUserByUsernameShouldReturnNullWhenUsernameBlank() {
        assertThat(userQueryService.findActiveUserByUsername(" ")).isNull();
        verify(userMapper, never()).selectOne(any());
    }

    @Test
    void findActiveUserByUsernameShouldReturnUserWhenFound() {
        UserEntity user = new UserEntity();
        user.setId(2L);
        user.setUsername("alice");
        when(userMapper.selectOne(any())).thenReturn(user);

        UserEntity found = userQueryService.findActiveUserByUsername("alice");

        assertThat(found).isNotNull();
        assertThat(found.getId()).isEqualTo(2L);
        assertThat(found.getUsername()).isEqualTo("alice");
    }

    @Test
    void findActiveUserByEmailShouldReturnUserWhenFound() {
        UserEntity user = new UserEntity();
        user.setId(3L);
        user.setEmail("alice@example.com");
        when(userMapper.selectOne(any())).thenReturn(user);

        UserEntity found = userQueryService.findActiveUserByEmail("alice@example.com");

        assertThat(found).isNotNull();
        assertThat(found.getEmail()).isEqualTo("alice@example.com");
    }

    @Test
    void findActiveUserByEmailShouldReturnNullWhenEmailBlank() {
        assertThat(userQueryService.findActiveUserByEmail(" ")).isNull();
        verify(userMapper, never()).selectOne(any());
    }

    @Test
    void existsActiveUserByEmailExcludeUserIdShouldReturnFalseWhenEmailBlank() {
        boolean exists = userQueryService.existsActiveUserByEmailExcludeUserId(" ", 1L);

        assertThat(exists).isFalse();
        verify(userMapper, never()).selectCount(any());
    }

    @Test
    void existsActiveUserByEmailExcludeUserIdShouldReturnTrueWhenCountPositive() {
        when(userMapper.selectCount(any())).thenReturn(1L);

        boolean exists = userQueryService.existsActiveUserByEmailExcludeUserId("alice@example.com", 1L);

        assertThat(exists).isTrue();
    }

    @Test
    void existsActiveUserByEmailExcludeUserIdShouldReturnFalseWhenCountZero() {
        when(userMapper.selectCount(any())).thenReturn(0L);

        boolean exists = userQueryService.existsActiveUserByEmailExcludeUserId("alice@example.com", 1L);

        assertThat(exists).isFalse();
    }
}
