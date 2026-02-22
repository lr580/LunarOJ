package com.lunaroj.auth.init;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lunaroj.auth.constant.PermissionGroupNames;
import com.lunaroj.auth.entity.PermissionGroupEntity;
import com.lunaroj.auth.mapper.PermissionGroupMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
@Order(10)
@RequiredArgsConstructor
public class PermissionGroupInitializer implements ApplicationRunner {

    private static final Map<String, String> BUILT_IN_GROUPS = new LinkedHashMap<>();

    static {
        BUILT_IN_GROUPS.put(PermissionGroupNames.USER, "普通用户");
        BUILT_IN_GROUPS.put(PermissionGroupNames.ADMIN, "管理员");
        BUILT_IN_GROUPS.put(PermissionGroupNames.ROOT, "超级管理员");
    }

    private final PermissionGroupMapper permissionGroupMapper;

    @Override
    public void run(ApplicationArguments args) {
        List<String> targetNames = List.copyOf(BUILT_IN_GROUPS.keySet());
        Set<String> existingNames = permissionGroupMapper.selectList(
                        Wrappers.<PermissionGroupEntity>lambdaQuery()
                                .select(PermissionGroupEntity::getName)
                                .in(PermissionGroupEntity::getName, targetNames)
                ).stream()
                .map(PermissionGroupEntity::getName)
                .collect(Collectors.toSet());

        int insertedCount = 0;
        for (String groupName : targetNames) {
            if (existingNames.contains(groupName)) {
                continue;
            }
            PermissionGroupEntity entity = new PermissionGroupEntity();
            entity.setName(groupName);
            entity.setPermissions("[]");
            entity.setDescription(BUILT_IN_GROUPS.get(groupName));
            entity.setIsBuiltIn(Boolean.TRUE);
            try {
                permissionGroupMapper.insert(entity);
                insertedCount++;
            } catch (DuplicateKeyException ignored) {
                // Another instance may initialize concurrently.
            }
        }

        if (insertedCount > 0) {
            log.info("已初始化 {} 个内置权限组: {}", insertedCount, targetNames);
        } else {
            log.info("内置权限组已存在: {}", targetNames);
        }
    }
}
