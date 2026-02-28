package com.lunaroj.migration.source;

import com.lunaroj.migration.model.SourceDatabaseConfig;
import com.lunaroj.migration.module.user.ScnuojUserRecord;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.util.StringUtils;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

public class ScnuojSourceClient implements AutoCloseable {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public ScnuojSourceClient(SourceDatabaseConfig sourceConfig) {
        validateConfig(sourceConfig);
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
        dataSource.setUrl(buildJdbcUrl(sourceConfig));
        dataSource.setUsername(sourceConfig.getUser());
        dataSource.setPassword(sourceConfig.getPassword());
        this.jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
    }

    public void testConnection() {
        jdbcTemplate.getJdbcTemplate().queryForObject("SELECT 1", Integer.class);
    }

    public long countUsers(List<Integer> statuses) {
        String sql = "SELECT COUNT(*) FROM `user` u WHERE u.status IN (:statuses)";
        MapSqlParameterSource params = new MapSqlParameterSource("statuses", statuses);
        Long count = jdbcTemplate.queryForObject(sql, params, Long.class);
        return count == null ? 0L : count;
    }

    public List<ScnuojUserRecord> fetchUsers(List<Integer> statuses, boolean importProfile, int offset, int limit) {
        String profileSelect = importProfile ? ", p.personal_intro AS personalIntro " : ", NULL AS personalIntro ";
        String profileJoin = importProfile ? "LEFT JOIN `user_profile` p ON p.user_id = u.id " : "";
        String sql = "SELECT u.id, u.username, u.nickname, u.email, u.role, u.status, " +
                "u.is_verify_email AS isVerifyEmail, u.created_at AS createdAt, u.updated_at AS updatedAt " +
                profileSelect +
                "FROM `user` u " +
                profileJoin +
                "WHERE u.status IN (:statuses) " +
                "ORDER BY u.id ASC LIMIT :limit OFFSET :offset";

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("statuses", statuses)
                .addValue("limit", limit)
                .addValue("offset", offset);
        return jdbcTemplate.query(sql, params, rowMapper());
    }

    private RowMapper<ScnuojUserRecord> rowMapper() {
        return (rs, rowNum) -> {
            ScnuojUserRecord record = new ScnuojUserRecord();
            record.setId(rs.getLong("id"));
            record.setUsername(rs.getString("username"));
            record.setNickname(rs.getString("nickname"));
            record.setEmail(rs.getString("email"));
            record.setRole(rs.getObject("role", Integer.class));
            record.setStatus(rs.getObject("status", Integer.class));
            record.setIsVerifyEmail(rs.getObject("isVerifyEmail", Integer.class));
            record.setPersonalIntro(rs.getString("personalIntro"));
            record.setCreatedAt(toLocalDateTime(rs.getTimestamp("createdAt")));
            record.setUpdatedAt(toLocalDateTime(rs.getTimestamp("updatedAt")));
            return record;
        };
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        if (timestamp == null) {
            return null;
        }
        return timestamp.toLocalDateTime();
    }

    private void validateConfig(SourceDatabaseConfig sourceConfig) {
        if (!StringUtils.hasText(sourceConfig.getHost())
                || sourceConfig.getPort() == null
                || !StringUtils.hasText(sourceConfig.getDb())
                || !StringUtils.hasText(sourceConfig.getUser())
                || sourceConfig.getPassword() == null) {
            throw new IllegalArgumentException("source database config is incomplete");
        }
    }

    private String buildJdbcUrl(SourceDatabaseConfig sourceConfig) {
        return "jdbc:mysql://"
                + sourceConfig.getHost()
                + ":"
                + sourceConfig.getPort()
                + "/"
                + sourceConfig.getDb()
                + "?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai"
                + "&useSSL=false&allowPublicKeyRetrieval=true";
    }

    @Override
    public void close() {
        // DriverManagerDataSource does not need explicit close.
    }
}

