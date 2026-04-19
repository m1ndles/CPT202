package com.cpt202.auth.repository;

import com.cpt202.auth.dto.ResourceAppealMessageResponse;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

/**
 * Resource report thread data access.
 */
@Repository
public class ResourceReportRepository {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final JdbcTemplate jdbcTemplate;

    public ResourceReportRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<ResourceReportThreadRecord> findById(Long threadId) {
        List<ResourceReportThreadRecord> items = jdbcTemplate.query("""
                SELECT t.id,
                       t.resource_id,
                       t.reporter_user_id,
                       t.reporter_name,
                       t.status,
                       t.created_at,
                       t.updated_at,
                       r.title AS resource_title,
                       r.status AS resource_status,
                       r.owner_username
                FROM resource_report_threads t
                JOIN heritage_resources r ON r.id = t.resource_id
                WHERE t.id = ?
                """, this::mapThread, threadId);
        return items.stream().findFirst();
    }

    public Optional<ResourceReportThreadRecord> findByResourceIdAndReporterUserId(Long resourceId, Long reporterUserId) {
        List<ResourceReportThreadRecord> items = jdbcTemplate.query("""
                SELECT t.id,
                       t.resource_id,
                       t.reporter_user_id,
                       t.reporter_name,
                       t.status,
                       t.created_at,
                       t.updated_at,
                       r.title AS resource_title,
                       r.status AS resource_status,
                       r.owner_username
                FROM resource_report_threads t
                JOIN heritage_resources r ON r.id = t.resource_id
                WHERE t.resource_id = ? AND t.reporter_user_id = ?
                """, this::mapThread, resourceId, reporterUserId);
        return items.stream().findFirst();
    }

    public List<ResourceReportThreadRecord> findAll() {
        return jdbcTemplate.query("""
                SELECT t.id,
                       t.resource_id,
                       t.reporter_user_id,
                       t.reporter_name,
                       t.status,
                       t.created_at,
                       t.updated_at,
                       r.title AS resource_title,
                       r.status AS resource_status,
                       r.owner_username
                FROM resource_report_threads t
                JOIN heritage_resources r ON r.id = t.resource_id
                ORDER BY t.updated_at DESC, t.id DESC
                """, this::mapThread);
    }

    public Long createThread(Long resourceId,
                             Long reporterUserId,
                             String reporterName,
                             String status,
                             LocalDateTime createdAt) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            var statement = connection.prepareStatement("""
                    INSERT INTO resource_report_threads
                    (resource_id, reporter_user_id, reporter_name, status, created_at, updated_at)
                    VALUES (?, ?, ?, ?, ?, ?)
                    """, new String[]{"id"});
            statement.setLong(1, resourceId);
            statement.setLong(2, reporterUserId);
            statement.setString(3, reporterName);
            statement.setString(4, status);
            statement.setTimestamp(5, Timestamp.valueOf(createdAt));
            statement.setTimestamp(6, Timestamp.valueOf(createdAt));
            return statement;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("Failed to create resource report thread.");
        }
        return key.longValue();
    }

    public void updateThreadStatus(Long threadId, String status, LocalDateTime updatedAt) {
        jdbcTemplate.update("""
                UPDATE resource_report_threads
                SET status = ?, updated_at = ?
                WHERE id = ?
                """, status, Timestamp.valueOf(updatedAt), threadId);
    }

    public List<ResourceAppealMessageResponse> findMessagesByThreadId(Long threadId) {
        return jdbcTemplate.query("""
                SELECT id, sender_role, sender_name, content, created_at
                FROM resource_report_messages
                WHERE thread_id = ?
                ORDER BY created_at ASC, id ASC
                """, (rs, rowNum) -> new ResourceAppealMessageResponse(
                rs.getLong("id"),
                rs.getString("sender_role"),
                rs.getString("sender_name"),
                rs.getString("content"),
                format(rs.getTimestamp("created_at"))
        ), threadId);
    }

    public Long insertMessage(Long threadId,
                              String senderRole,
                              String senderName,
                              String content,
                              LocalDateTime createdAt) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            var statement = connection.prepareStatement("""
                    INSERT INTO resource_report_messages
                    (thread_id, sender_role, sender_name, content, created_at)
                    VALUES (?, ?, ?, ?, ?)
                    """, new String[]{"id"});
            statement.setLong(1, threadId);
            statement.setString(2, senderRole);
            statement.setString(3, senderName);
            statement.setString(4, content);
            statement.setTimestamp(5, Timestamp.valueOf(createdAt));
            return statement;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("Failed to create resource report message.");
        }
        return key.longValue();
    }

    private ResourceReportThreadRecord mapThread(ResultSet rs, int rowNum) throws SQLException {
        return new ResourceReportThreadRecord(
                rs.getLong("id"),
                rs.getLong("resource_id"),
                rs.getLong("reporter_user_id"),
                rs.getString("reporter_name"),
                rs.getString("status"),
                toLocalDateTime(rs.getTimestamp("created_at")),
                toLocalDateTime(rs.getTimestamp("updated_at")),
                rs.getString("resource_title"),
                rs.getString("resource_status"),
                rs.getString("owner_username")
        );
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private String format(Timestamp timestamp) {
        if (timestamp == null) {
            return null;
        }
        return DATE_TIME_FORMATTER.format(timestamp.toLocalDateTime());
    }

    public record ResourceReportThreadRecord(
            Long id,
            Long resourceId,
            Long reporterUserId,
            String reporterName,
            String status,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            String resourceTitle,
            String resourceStatus,
            String ownerUsername
    ) {
    }
}
