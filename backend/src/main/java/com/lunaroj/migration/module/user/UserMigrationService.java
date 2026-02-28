package com.lunaroj.migration.module.user;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lunaroj.constant.PermissionGroupNames;
import com.lunaroj.mapper.UserMapper;
import com.lunaroj.migration.model.ExecutionConfig;
import com.lunaroj.migration.model.MigrationProfile;
import com.lunaroj.migration.model.SourceDatabaseConfig;
import com.lunaroj.migration.source.ScnuojSourceClient;
import com.lunaroj.model.entity.UserEntity;
import com.lunaroj.service.PermissionGroupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserMigrationService {

    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9._-]{3,64}$");
    private static final DateTimeFormatter JOB_ID_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS");

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final PermissionGroupService permissionGroupService;

    public void testConnection(SourceDatabaseConfig sourceConfig) {
        try (ScnuojSourceClient sourceClient = new ScnuojSourceClient(sourceConfig)) {
            sourceClient.testConnection();
        }
    }

    public UserMigrationReport migrate(MigrationProfile profile) {
        UserModuleConfig moduleConfig = profile.getModules().getUser();
        if (moduleConfig == null || !Boolean.TRUE.equals(moduleConfig.getEnabled())) {
            throw new IllegalArgumentException("user module is not enabled");
        }
        validateModuleConfig(moduleConfig);

        ExecutionConfig executionConfig = profile.getExecution();
        int batchSize = safeBatchSize(executionConfig.getBatchSize());
        boolean dryRun = Boolean.TRUE.equals(executionConfig.getDryRun());
        boolean failFast = Boolean.TRUE.equals(executionConfig.getFailFast());
        int issueLimit = safeIssueLimit(executionConfig.getIssueLimit());

        UserMigrationReport report = new UserMigrationReport();
        report.setJobId(LocalDateTime.now().format(JOB_ID_FORMATTER));
        report.setDryRun(dryRun);
        report.setStartedAt(LocalDateTime.now());

        Long rootGroupId = permissionGroupService.getGroupIdByName(PermissionGroupNames.ROOT);
        Long adminGroupId = permissionGroupService.getGroupIdByName(PermissionGroupNames.ADMIN);
        Long userGroupId = permissionGroupService.getGroupIdByName(PermissionGroupNames.USER);

        try (ScnuojSourceClient sourceClient = new ScnuojSourceClient(profile.getSource())) {
            long total = sourceClient.countUsers(moduleConfig.getStatusFilter());
            report.setSourceTotal(total);

            int offset = 0;
            while (true) {
                List<ScnuojUserRecord> records = sourceClient.fetchUsers(
                        moduleConfig.getStatusFilter(),
                        Boolean.TRUE.equals(moduleConfig.getImportProfile()),
                        offset,
                        batchSize
                );
                if (records.isEmpty()) {
                    break;
                }

                for (ScnuojUserRecord record : records) {
                    try {
                        processOneRecord(record, moduleConfig, dryRun, rootGroupId, adminGroupId, userGroupId, report, issueLimit);
                    } catch (Exception ex) {
                        report.setFailed(report.getFailed() + 1);
                        addIssue(report, issueLimit, new UserMigrationIssue(
                                record.getId(),
                                record.getUsername(),
                                "failed",
                                ex.getMessage()
                        ));
                        if (failFast) {
                            throw ex;
                        }
                    }
                    report.setProcessed(report.getProcessed() + 1);
                }

                offset += records.size();
                log.info("User migration progress: {}/{}", report.getProcessed(), total);
            }
        } catch (Exception ex) {
            report.setFinishedAt(LocalDateTime.now());
            log.error("User migration failed. jobId={}", report.getJobId(), ex);
            throw new IllegalStateException("user migration failed: " + ex.getMessage(), ex);
        }

        report.setFinishedAt(LocalDateTime.now());
        return report;
    }

    private void processOneRecord(
            ScnuojUserRecord record,
            UserModuleConfig config,
            boolean dryRun,
            Long rootGroupId,
            Long adminGroupId,
            Long userGroupId,
            UserMigrationReport report,
            int issueLimit
    ) {
        String username = normalize(record.getUsername());
        if (!isValidUsername(username)) {
            if (config.getConflictPolicy() == UsernameConflictPolicy.RENAME) {
                username = generateRenamedUsername(record.getId());
                username = ensureUniqueUsername(username);
            } else {
                report.setSkipped(report.getSkipped() + 1);
                addIssue(report, issueLimit, new UserMigrationIssue(record.getId(), record.getUsername(), "skipped", "invalid username"));
                return;
            }
        }

        UserEntity existingByUsername = findActiveByUsername(username);
        if (existingByUsername != null) {
            if (config.getConflictPolicy() == UsernameConflictPolicy.SKIP) {
                report.setSkipped(report.getSkipped() + 1);
                addIssue(report, issueLimit, new UserMigrationIssue(record.getId(), username, "skipped", "username conflict"));
                return;
            }
            if (config.getConflictPolicy() == UsernameConflictPolicy.RENAME) {
                username = ensureUniqueUsername(generateRenamedUsername(record.getId()));
                addIssue(report, issueLimit, new UserMigrationIssue(record.getId(), record.getUsername(), "renamed", "username conflict"));
                existingByUsername = null;
            } else if (config.getConflictPolicy() == UsernameConflictPolicy.OVERWRITE_SAFE) {
                if (isSafeOverwrite(existingByUsername, normalize(record.getEmail()))) {
                    if (!dryRun) {
                        updateExistingUser(existingByUsername.getId(), record, existingByUsername.getUsername(), rootGroupId, adminGroupId, userGroupId, config);
                    }
                    report.setUpdated(report.getUpdated() + 1);
                    addIssue(report, issueLimit, new UserMigrationIssue(record.getId(), username, "updated", "safe overwrite"));
                    return;
                }
                report.setSkipped(report.getSkipped() + 1);
                addIssue(report, issueLimit, new UserMigrationIssue(record.getId(), username, "skipped", "username conflict not safe to overwrite"));
                return;
            }
        }

        String email = normalize(record.getEmail());
        if (StringUtils.hasText(email) && email.length() > 191) {
            email = null;
            addIssue(report, issueLimit, new UserMigrationIssue(record.getId(), username, "email_nullified", "email too long"));
        }

        if (StringUtils.hasText(email)) {
            UserEntity existingByEmail = findActiveByEmail(email);
            if (existingByEmail != null) {
                if (config.getEmailConflictPolicy() == EmailConflictPolicy.SKIP_USER) {
                    report.setSkipped(report.getSkipped() + 1);
                    addIssue(report, issueLimit, new UserMigrationIssue(record.getId(), username, "skipped", "email conflict"));
                    return;
                }
                email = null;
                addIssue(report, issueLimit, new UserMigrationIssue(record.getId(), username, "email_nullified", "email conflict"));
            }
        }

        if (!dryRun) {
            UserEntity newUser = toNewUserEntity(record, username, email, rootGroupId, adminGroupId, userGroupId, config);
            userMapper.insert(newUser);
        }
        report.setInserted(report.getInserted() + 1);
    }

    private UserEntity toNewUserEntity(
            ScnuojUserRecord record,
            String username,
            String email,
            Long rootGroupId,
            Long adminGroupId,
            Long userGroupId,
            UserModuleConfig config
    ) {
        UserEntity entity = new UserEntity();
        entity.setUsername(username);
        entity.setPassword(buildEncodedPassword(config));
        entity.setNickname(resolveNickname(record.getNickname(), username));
        entity.setEmail(email);
        entity.setEmailVerified(record.getIsVerifyEmail() != null && record.getIsVerifyEmail() == 1);
        entity.setPermissionGroupId(resolvePermissionGroupId(record.getRole(), rootGroupId, adminGroupId, userGroupId));
        entity.setProfile(Boolean.TRUE.equals(config.getImportProfile()) ? normalize(record.getPersonalIntro()) : null);
        entity.setDefaultCodePublic(Boolean.FALSE);
        entity.setCreatedAt(record.getCreatedAt() == null ? LocalDateTime.now() : record.getCreatedAt());
        entity.setUpdatedAt(record.getUpdatedAt() == null ? LocalDateTime.now() : record.getUpdatedAt());
        entity.setDeletedAt(null);
        return entity;
    }

    private void updateExistingUser(
            Long userId,
            ScnuojUserRecord record,
            String username,
            Long rootGroupId,
            Long adminGroupId,
            Long userGroupId,
            UserModuleConfig config
    ) {
        UserEntity entity = new UserEntity();
        entity.setId(userId);
        entity.setPassword(buildEncodedPassword(config));
        entity.setNickname(resolveNickname(record.getNickname(), username));
        entity.setEmail(normalizeEmailForUpdate(record.getEmail()));
        entity.setEmailVerified(record.getIsVerifyEmail() != null && record.getIsVerifyEmail() == 1);
        entity.setPermissionGroupId(resolvePermissionGroupId(record.getRole(), rootGroupId, adminGroupId, userGroupId));
        entity.setProfile(Boolean.TRUE.equals(config.getImportProfile()) ? normalize(record.getPersonalIntro()) : null);
        entity.setDefaultCodePublic(Boolean.FALSE);
        entity.setUpdatedAt(record.getUpdatedAt() == null ? LocalDateTime.now() : record.getUpdatedAt());
        userMapper.updateById(entity);
    }

    private String normalizeEmailForUpdate(String sourceEmail) {
        String email = normalize(sourceEmail);
        if (!StringUtils.hasText(email)) {
            return null;
        }
        return email.length() <= 191 ? email : null;
    }

    private UserEntity findActiveByUsername(String username) {
        if (!StringUtils.hasText(username)) {
            return null;
        }
        return userMapper.selectOne(
                Wrappers.<UserEntity>lambdaQuery()
                        .eq(UserEntity::getUsername, username)
                        .isNull(UserEntity::getDeletedAt)
                        .last("LIMIT 1")
        );
    }

    private UserEntity findActiveByEmail(String email) {
        if (!StringUtils.hasText(email)) {
            return null;
        }
        return userMapper.selectOne(
                Wrappers.<UserEntity>lambdaQuery()
                        .eq(UserEntity::getEmail, email)
                        .isNull(UserEntity::getDeletedAt)
                        .last("LIMIT 1")
        );
    }

    private String ensureUniqueUsername(String baseUsername) {
        String candidate = baseUsername;
        int suffix = 1;
        while (findActiveByUsername(candidate) != null) {
            String suffixText = "_" + suffix;
            int maxBaseLength = 64 - suffixText.length();
            String head = baseUsername.length() > maxBaseLength ? baseUsername.substring(0, maxBaseLength) : baseUsername;
            candidate = head + suffixText;
            suffix++;
        }
        return candidate;
    }

    private String generateRenamedUsername(Long sourceId) {
        String idPart = sourceId == null ? UUID.randomUUID().toString().replace("-", "").substring(0, 10) : String.valueOf(sourceId);
        String candidate = "legacy_" + idPart;
        if (candidate.length() > 64) {
            return candidate.substring(0, 64);
        }
        return candidate;
    }

    private String resolveNickname(String sourceNickname, String fallbackUsername) {
        String nickname = normalize(sourceNickname);
        if (!StringUtils.hasText(nickname)) {
            nickname = fallbackUsername;
        }
        if (nickname == null) {
            return "legacy_user";
        }
        if (nickname.length() > 64) {
            return nickname.substring(0, 64);
        }
        return nickname;
    }

    private String buildEncodedPassword(UserModuleConfig config) {
        String rawPassword;
        if (config.getPasswordMode() == UserPasswordMode.FIXED) {
            rawPassword = config.getFixedPassword();
        } else {
            rawPassword = UUID.randomUUID().toString() + "#" + UUID.randomUUID();
        }
        return passwordEncoder.encode(rawPassword);
    }

    private Long resolvePermissionGroupId(Integer sourceRole, Long rootGroupId, Long adminGroupId, Long userGroupId) {
        if (sourceRole != null) {
            if (sourceRole == 30) {
                return rootGroupId;
            }
            if (sourceRole == 20) {
                return adminGroupId;
            }
        }
        return userGroupId;
    }

    private boolean isSafeOverwrite(UserEntity existing, String sourceEmail) {
        String existingEmail = normalize(existing.getEmail());
        String normalizedSourceEmail = normalize(sourceEmail);
        return Objects.equals(existingEmail, normalizedSourceEmail);
    }

    private boolean isValidUsername(String username) {
        return StringUtils.hasText(username) && USERNAME_PATTERN.matcher(username).matches();
    }

    private String normalize(String text) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        return text.trim();
    }

    private int safeBatchSize(Integer batchSize) {
        if (batchSize == null || batchSize <= 0) {
            return 500;
        }
        return batchSize;
    }

    private int safeIssueLimit(Integer issueLimit) {
        if (issueLimit == null || issueLimit <= 0) {
            return 200;
        }
        return issueLimit;
    }

    private void addIssue(UserMigrationReport report, int issueLimit, UserMigrationIssue issue) {
        if (report.getIssues().size() >= issueLimit) {
            report.setIssueLimitReached(true);
            return;
        }
        report.getIssues().add(issue);
    }

    private void validateModuleConfig(UserModuleConfig config) {
        if (config.getPasswordMode() == UserPasswordMode.FIXED && !StringUtils.hasText(config.getFixedPassword())) {
            throw new IllegalArgumentException("fixed password is required when password mode is FIXED");
        }
        if (config.getStatusFilter() == null || config.getStatusFilter().isEmpty()) {
            throw new IllegalArgumentException("status filter cannot be empty");
        }
        config.getStatusFilter().replaceAll(value -> value == null ? 10 : value);
        if (config.getConflictPolicy() == null) {
            config.setConflictPolicy(UsernameConflictPolicy.SKIP);
        }
        if (config.getEmailConflictPolicy() == null) {
            config.setEmailConflictPolicy(EmailConflictPolicy.NULLIFY);
        }
        if (config.getPasswordMode() == null) {
            config.setPasswordMode(UserPasswordMode.RANDOM_UNLOGIN);
        }
        if (config.getImportProfile() == null) {
            config.setImportProfile(Boolean.FALSE);
        }
        if (config.getEnabled() == null) {
            config.setEnabled(Boolean.TRUE);
        }
    }
}
