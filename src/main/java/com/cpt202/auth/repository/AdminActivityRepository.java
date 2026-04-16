package com.cpt202.auth.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AdminActivityRepository {

    private final JdbcTemplate jdbcTemplate;

    public AdminActivityRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void insert(String actionType,
                       String targetType,
                       String targetName,
                       String operatorName,
                       LocalDateTime createdAt,
                       String details) {
        jdbcTemplate.update(
                """
                INSERT INTO admin_activity_history
                (action_type, target_type, target_name, operator_name, created_at, details)
                VALUES (?, ?, ?, ?, ?, ?)
                """,
                actionType,
                targetType,
                targetName,
                operatorName,
                Timestamp.valueOf(createdAt),
                details
        );
    }

    public List<ActivityRecord> findAll() {
        return jdbcTemplate.query(
                """
                SELECT id, action_type, target_type, target_name, operator_name, created_at, details
                FROM admin_activity_history
                ORDER BY created_at DESC, id DESC
                """,
                this::mapRecord
        );
    }

    public long count() {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM admin_activity_history", Long.class);
        return count == null ? 0 : count;
    }

    private ActivityRecord mapRecord(ResultSet rs, int rowNum) throws SQLException {
        Timestamp createdAt = rs.getTimestamp("created_at");
        return new ActivityRecord(
                rs.getLong("id"),
                rs.getString("action_type"),
                rs.getString("target_type"),
                rs.getString("target_name"),
                rs.getString("operator_name"),
                createdAt == null ? null : createdAt.toLocalDateTime(),
                rs.getString("details")
        );
    }

    public record ActivityRecord(
            Long id,
            String actionType,
            String targetType,
            String targetName,
            String operatorName,
            LocalDateTime createdAt,
            String details
    ) {
    }
}
