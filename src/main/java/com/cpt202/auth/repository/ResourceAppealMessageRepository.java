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
 * Appeal thread data access.
 */
@Repository
public class ResourceAppealMessageRepository {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final JdbcTemplate jdbcTemplate;

    public ResourceAppealMessageRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<ResourceAppealMessageResponse> findByResourceId(Long resourceId) {
        return jdbcTemplate.query(
                """
                SELECT id, sender_role, sender_name, content, created_at
                FROM resource_appeal_messages
                WHERE resource_id = ?
                ORDER BY created_at ASC, id ASC
                """,
                (rs, rowNum) -> {
                    Timestamp createdAt = rs.getTimestamp("created_at");
                    return new ResourceAppealMessageResponse(
                            rs.getLong("id"),
                            rs.getString("sender_role"),
                            rs.getString("sender_name"),
                            rs.getString("content"),
                            createdAt == null ? "" : DATE_TIME_FORMATTER.format(createdAt.toLocalDateTime())
                    );
                },
                resourceId
        );
    }

    public Long insert(Long resourceId,
                       String senderRole,
                       String senderName,
                       String content,
                       LocalDateTime createdAt) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            var statement = connection.prepareStatement(
                    """
                    INSERT INTO resource_appeal_messages (resource_id, sender_role, sender_name, content, created_at)
                    VALUES (?, ?, ?, ?, ?)
                    """,
                    new String[]{"id"}
            );
            statement.setLong(1, resourceId);
            statement.setString(2, senderRole);
            statement.setString(3, senderName);
            statement.setString(4, content);
            statement.setTimestamp(5, Timestamp.valueOf(createdAt));
            return statement;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("Failed to create appeal message.");
        }
        return key.longValue();
    }
}
