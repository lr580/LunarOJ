package com.lunaroj.auth.init;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lunaroj.auth.constant.PermissionGroupNames;
import com.lunaroj.auth.entity.UserEntity;
import com.lunaroj.auth.mapper.UserMapper;
import com.lunaroj.auth.service.PermissionGroupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Order(20)
@RequiredArgsConstructor
public class RootAccountInitializer implements ApplicationRunner {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final PermissionGroupService permissionGroupService;
    private final RootAccountInitProperties rootAccountInitProperties;

    @Override
    public void run(ApplicationArguments args) {
        if (!rootAccountInitProperties.isEnabled()) {
            log.info("ROOT 账号自动初始化未启用，跳过");
            return;
        }

        Long rootGroupId = permissionGroupService.getGroupIdByName(PermissionGroupNames.ROOT);
        if (hasActiveRootAccount(rootGroupId)) {
            log.info("已存在 ROOT 账号，跳过 ROOT 账号初始化");
            return;
        }

        String username = rootAccountInitProperties.resolveUsername();
        String rawPassword = rootAccountInitProperties.resolvePassword();

        if (hasActiveUserByUsername(username)) {
            log.warn("初始化 ROOT 账号失败：用户名 {} 已存在，请修改 lunaroj.init.root.username 配置", username);
            return;
        }

        UserEntity rootUser = new UserEntity();
        rootUser.setUsername(username);
        rootUser.setPassword(passwordEncoder.encode(rawPassword));
        rootUser.setNickname(username);
        rootUser.setEmail(null);
        rootUser.setEmailVerified(Boolean.FALSE);
        rootUser.setPermissionGroupId(rootGroupId);
        rootUser.setDefaultCodePublic(Boolean.FALSE);

        try {
            userMapper.insert(rootUser);
            log.info("已自动创建 ROOT 账号: username={}", username);
        } catch (DuplicateKeyException ignored) {
            log.warn("初始化 ROOT 账号发生并发冲突，可能已由其他实例创建: username={}", username);
        }
    }

    private boolean hasActiveRootAccount(Long rootGroupId) {
        Long count = userMapper.selectCount(
                Wrappers.<UserEntity>lambdaQuery()
                        .eq(UserEntity::getPermissionGroupId, rootGroupId)
                        .isNull(UserEntity::getDeletedAt)
        );
        return count != null && count > 0;
    }

    private boolean hasActiveUserByUsername(String username) {
        Long count = userMapper.selectCount(
                Wrappers.<UserEntity>lambdaQuery()
                        .eq(UserEntity::getUsername, username)
                        .isNull(UserEntity::getDeletedAt)
        );
        return count != null && count > 0;
    }

}
