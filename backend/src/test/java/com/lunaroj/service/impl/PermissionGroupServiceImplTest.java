package com.lunaroj.service.impl;

import com.lunaroj.common.error.ErrorCode;
import com.lunaroj.common.exception.BusinessException;
import com.lunaroj.mapper.PermissionGroupMapper;
import com.lunaroj.model.entity.PermissionGroupEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PermissionGroupServiceImplTest {

    @Mock
    private PermissionGroupMapper permissionGroupMapper;

    private PermissionGroupServiceImpl permissionGroupService;

    @BeforeEach
    void setUp() {
        permissionGroupService = new PermissionGroupServiceImpl(permissionGroupMapper);
    }

    @Test
    void getGroupIdByNameShouldThrowWhenGroupNameBlank() {
        assertThatThrownBy(() -> permissionGroupService.getGroupIdByName("  "))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.BAD_REQUEST));

        verify(permissionGroupMapper, never()).selectOne(any());
    }

    @Test
    void getGroupIdByNameShouldThrowWhenGroupMissing() {
        when(permissionGroupMapper.selectOne(any())).thenReturn(null);

        assertThatThrownBy(() -> permissionGroupService.getGroupIdByName("USER"))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.INTERNAL_ERROR));
    }

    @Test
    void getGroupIdByNameShouldReturnGroupIdWhenFound() {
        PermissionGroupEntity entity = new PermissionGroupEntity();
        entity.setId(1001L);
        entity.setName("USER");
        when(permissionGroupMapper.selectOne(any())).thenReturn(entity);

        Long groupId = permissionGroupService.getGroupIdByName("USER");

        assertThat(groupId).isEqualTo(1001L);
    }

    @Test
    void getGroupDisplayNameByIdShouldReturnNullWhenGroupIdNull() {
        assertThat(permissionGroupService.getGroupDisplayNameById(null)).isNull();
        verify(permissionGroupMapper, never()).selectById(any());
    }

    @Test
    void getGroupDisplayNameByIdShouldReturnDescriptionWhenNotBlank() {
        PermissionGroupEntity entity = new PermissionGroupEntity();
        entity.setId(2L);
        entity.setName("USER");
        entity.setDescription("普通用户");
        when(permissionGroupMapper.selectById(2L)).thenReturn(entity);

        String displayName = permissionGroupService.getGroupDisplayNameById(2L);

        assertThat(displayName).isEqualTo("普通用户");
    }

    @Test
    void getGroupDisplayNameByIdShouldFallbackToNameWhenDescriptionBlank() {
        PermissionGroupEntity entity = new PermissionGroupEntity();
        entity.setId(2L);
        entity.setName("USER");
        entity.setDescription("  ");
        when(permissionGroupMapper.selectById(2L)).thenReturn(entity);

        String displayName = permissionGroupService.getGroupDisplayNameById(2L);

        assertThat(displayName).isEqualTo("USER");
    }

    @Test
    void getGroupNameByIdShouldReturnNullWhenGroupIdNull() {
        assertThat(permissionGroupService.getGroupNameById(null)).isNull();
        verify(permissionGroupMapper, never()).selectById(any());
    }

    @Test
    void getGroupNameByIdShouldReturnNullWhenGroupMissing() {
        when(permissionGroupMapper.selectById(2L)).thenReturn(null);

        assertThat(permissionGroupService.getGroupNameById(2L)).isNull();
    }

    @Test
    void getGroupNameByIdShouldReturnNameWhenFound() {
        PermissionGroupEntity entity = new PermissionGroupEntity();
        entity.setId(2L);
        entity.setName("USER");
        when(permissionGroupMapper.selectById(2L)).thenReturn(entity);

        String name = permissionGroupService.getGroupNameById(2L);

        assertThat(name).isEqualTo("USER");
    }
}
