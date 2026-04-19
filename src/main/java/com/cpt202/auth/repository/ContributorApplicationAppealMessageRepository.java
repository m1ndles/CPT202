package com.cpt202.auth.repository;

import com.cpt202.auth.dto.ResourceAppealMessageResponse;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

/**
 * Rejected contributor application appeal thread data access.
 */
@Repository
public class ContributorApplicationAppealMessageRepository {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final JdbcTemplate jdbcTemplate;

    public ContributorApplicationAppealMessageRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<ResourceAppealMessageResponse> findByApplicationId(Long applicationId) {
        return jdbcTemplate.query("""
                SELECT id, sender_role, sender_name, content, created_at
                FROM contributor_application_appeal_messages
                WHERE application_id = ?
                ORDER BY created_at ASC, id ASC
                """, (rs, rowNum) -> new ResourceAppealMessageResponse(
                rs.getLong("id"),
                rs.getString("sender_role"),
                rs.getString("sender_name"),
                rs.getString("content"),
                format(rs.getTimestamp("created_at"))
        ), applicationId);
    }

    public Long insert(Long applicationId,
                       String senderRole,
                       String senderName,
                       String content,
                       LocalDateTime createdAt) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            var statement = connection.prepareStatement("""
                    INSERT INTO contributor_application_appeal_messages
                    (application_id, sender_role, sender_name, content, created_at)
                    VALUES (?, ?, ?, ?, ?)
                    """, new String[]{"id"});
            statement.setLong(1, applicationId);
            statement.setString(2, senderRole);
            statement.setString(3, senderName);
            statement.setString(4, content);
            statement.setTimestamp(5, Timestamp.valueOf(createdAt));
            return statement;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("Failed to create contributor appeal message.");
        }
        return key.longValue();
    }

    private String format(Timestamp timestamp) {
        if (timestamp == null) {
            return null;
        }
        return DATE_TIME_FORMATTER.format(timestamp.toLocalDateTime());
    }
}
