package com.lunaroj.auth.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lunaroj.auth.entity.PermissionGroupEntity;
import com.lunaroj.auth.mapper.PermissionGroupMapper;
import com.lunaroj.auth.service.PermissionGroupService;
import com.lunaroj.common.BusinessException;
import com.lunaroj.common.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Caching;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionGroupServiceImpl implements PermissionGroupService {

    private final PermissionGroupMapper permissionGroupMapper;

    @Override
    @Cacheable(cacheNames = CACHE_NAME_GROUP_ID_BY_NAME, key = "#groupName")
    public Long getGroupIdByName(String groupName) {
        if (!StringUtils.hasText(groupName)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "权限组名不能为空");
        }
        return loadGroupIdByName(groupName);
    }

    @Override
    @Cacheable(cacheNames = CACHE_NAME_GROUP_DISPLAY_NAME_BY_ID, key = "#groupId")
    public String getGroupDisplayNameById(Long groupId) {
        if (groupId == null) {
            return null;
        }
        PermissionGroupEntity permissionGroupEntity = permissionGroupMapper.selectById(groupId);
        if (permissionGroupEntity == null) {
            return null;
        }
        if (StringUtils.hasText(permissionGroupEntity.getDescription())) {
            return permissionGroupEntity.getDescription();
        }
        return permissionGroupEntity.getName();
    }

    @Override
    @Cacheable(cacheNames = CACHE_NAME_GROUP_NAME_BY_ID, key = "#groupId")
    public String getGroupNameById(Long groupId) {
        if (groupId == null) {
            return null;
        }
        PermissionGroupEntity permissionGroupEntity = permissionGroupMapper.selectById(groupId);
        if (permissionGroupEntity == null) {
            return null;
        }
        return permissionGroupEntity.getName();
    }

    @Override
    @CacheEvict(
            cacheNames = CACHE_NAME_GROUP_ID_BY_NAME,
            key = "#groupName",
            condition = "#groupName != null && !#groupName.isBlank()"
    )
    public void evictGroupIdCache(String groupName) {
        // Cache eviction handled by annotation.
    }

    @Override
    @Caching(evict = {
            @CacheEvict(cacheNames = CACHE_NAME_GROUP_ID_BY_NAME, allEntries = true),
            @CacheEvict(cacheNames = CACHE_NAME_GROUP_DISPLAY_NAME_BY_ID, allEntries = true),
            @CacheEvict(cacheNames = CACHE_NAME_GROUP_NAME_BY_ID, allEntries = true)
    })
    public void clearPermissionGroupCache() {
        // Cache clear handled by annotation.
    }

    private Long loadGroupIdByName(String groupName) {
        PermissionGroupEntity permissionGroupEntity = permissionGroupMapper.selectOne(
                Wrappers.<PermissionGroupEntity>lambdaQuery()
                        .eq(PermissionGroupEntity::getName, groupName)
                        .last("LIMIT 1")
        );
        if (permissionGroupEntity == null) {
            throw new BusinessException(
                    ErrorCode.INTERNAL_ERROR,
                    "权限组 " + groupName + " 不存在，请先初始化 permission_group"
            );
        }
        Long groupId = permissionGroupEntity.getId();
        log.debug("Loaded permission group id from database: name={}, id={}", groupName, groupId);
        return groupId;
    }
}
