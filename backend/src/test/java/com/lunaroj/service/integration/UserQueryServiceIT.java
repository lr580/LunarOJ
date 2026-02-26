package com.lunaroj.service.integration;

import com.lunaroj.mapper.UserMapper;
import com.lunaroj.model.entity.UserEntity;
import com.lunaroj.service.UserQueryService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("integration")
class UserQueryServiceIT {

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private UserQueryService userQueryService;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long insertedUserId;
    private String insertedUsername;
    private String insertedEmail;

    @AfterEach
    void cleanupHardDelete() {
        if (insertedUserId != null) {
            jdbcTemplate.update("DELETE FROM `user` WHERE id = ?", insertedUserId);
            insertedUserId = null;
        } else if (insertedUsername != null) {
            jdbcTemplate.update("DELETE FROM `user` WHERE username = ?", insertedUsername);
        }
        insertedUsername = null;
        insertedEmail = null;
    }

    @Test
    void shouldQueryUserFromDatabaseAndHardDeleteAfterVerification() {
        Long userGroupId = jdbcTemplate.queryForObject(
                "SELECT id FROM permission_group WHERE name = 'USER' LIMIT 1",
                Long.class
        );
        assertThat(userGroupId).isNotNull();

        String suffix = UUID.randomUUID().toString().replace("-", "");
        insertedUsername = "it_user_" + suffix.substring(0, 12);
        insertedEmail = insertedUsername + "@example.com";

        UserEntity user = new UserEntity();
        user.setUsername(insertedUsername);
        user.setPassword("encoded-password");
        user.setNickname("IntegrationUser");
        user.setEmail(insertedEmail);
        user.setEmailVerified(Boolean.FALSE);
        user.setPermissionGroupId(userGroupId);
        user.setDefaultCodePublic(Boolean.FALSE);

        int affected = userMapper.insert(user);
        assertThat(affected).isEqualTo(1);
        insertedUserId = user.getId();
        assertThat(insertedUserId).isNotNull();

        UserEntity byId = userQueryService.findActiveUserById(insertedUserId);
        assertThat(byId).isNotNull();
        assertThat(byId.getUsername()).isEqualTo(insertedUsername);

        UserEntity byUsername = userQueryService.findActiveUserByUsername(insertedUsername);
        assertThat(byUsername).isNotNull();
        assertThat(byUsername.getId()).isEqualTo(insertedUserId);

        UserEntity byEmail = userQueryService.findActiveUserByEmail(insertedEmail);
        assertThat(byEmail).isNotNull();
        assertThat(byEmail.getId()).isEqualTo(insertedUserId);

        assertThat(userQueryService.existsActiveUserByEmailExcludeUserId(insertedEmail, null)).isTrue();
        assertThat(userQueryService.existsActiveUserByEmailExcludeUserId(insertedEmail, insertedUserId)).isFalse();

        Long persistedUserId = insertedUserId;
        int deleted = jdbcTemplate.update("DELETE FROM `user` WHERE id = ?", persistedUserId);
        assertThat(deleted).isEqualTo(1);
        insertedUserId = null;
        insertedUsername = null;
        insertedEmail = null;

        assertThat(userQueryService.findActiveUserById(persistedUserId)).isNull();
    }
}
