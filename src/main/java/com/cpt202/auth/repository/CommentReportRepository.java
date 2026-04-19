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
 * Comment report thread data access.
 */
@Repository
public class CommentReportRepository {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final JdbcTemplate jdbcTemplate;

    public CommentReportRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<CommentReportThreadRecord> findById(Long threadId) {
        List<CommentReportThreadRecord> items = jdbcTemplate.query("""
                SELECT t.id,
                       t.comment_id,
                       t.reporter_user_id,
                       t.reporter_name,
                       t.status,
                       t.created_at,
                       t.updated_at,
                       c.resource_id,
                       c.content AS comment_content,
                       u.username AS comment_author,
                       r.title AS resource_title,
                       r.status AS resource_status
                FROM comment_report_threads t
                JOIN comments c ON c.id = t.comment_id
                JOIN users u ON u.id = c.user_id
                JOIN heritage_resources r ON r.id = c.resource_id
                WHERE t.id = ?
                """, this::mapThread, threadId);
        return items.stream().findFirst();
    }

    public Optional<CommentReportThreadRecord> findByCommentIdAndReporterUserId(Long commentId, Long reporterUserId) {
        List<CommentReportThreadRecord> items = jdbcTemplate.query("""
                SELECT t.id,
                       t.comment_id,
                       t.reporter_user_id,
                       t.reporter_name,
                       t.status,
                       t.created_at,
                       t.updated_at,
                       c.resource_id,
                       c.content AS comment_content,
                       u.username AS comment_author,
                       r.title AS resource_title,
                       r.status AS resource_status
                FROM comment_report_threads t
                JOIN comments c ON c.id = t.comment_id
                JOIN users u ON u.id = c.user_id
                JOIN heritage_resources r ON r.id = c.resource_id
                WHERE t.comment_id = ? AND t.reporter_user_id = ?
                """, this::mapThread, commentId, reporterUserId);
        return items.stream().findFirst();
    }

    public List<CommentReportThreadRecord> findAll() {
        return jdbcTemplate.query("""
                SELECT t.id,
                       t.comment_id,
                       t.reporter_user_id,
                       t.reporter_name,
                       t.status,
                       t.created_at,
                       t.updated_at,
                       c.resource_id,
                       c.content AS comment_content,
                       u.username AS comment_author,
                       r.title AS resource_title,
                       r.status AS resource_status
                FROM comment_report_threads t
                JOIN comments c ON c.id = t.comment_id
                JOIN users u ON u.id = c.user_id
                JOIN heritage_resources r ON r.id = c.resource_id
                ORDER BY t.updated_at DESC, t.id DESC
                """, this::mapThread);
    }

    public Long createThread(Long commentId,
                             Long reporterUserId,
                             String reporterName,
                             String status,
                             LocalDateTime createdAt) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            var statement = connection.prepareStatement("""
                    INSERT INTO comment_report_threads
                    (comment_id, reporter_user_id, reporter_name, status, created_at, updated_at)
                    VALUES (?, ?, ?, ?, ?, ?)
                    """, new String[]{"id"});
            statement.setLong(1, commentId);
            statement.setLong(2, reporterUserId);
            statement.setString(3, reporterName);
            statement.setString(4, status);
            statement.setTimestamp(5, Timestamp.valueOf(createdAt));
            statement.setTimestamp(6, Timestamp.valueOf(createdAt));
            return statement;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("Failed to create comment report thread.");
        }
        return key.longValue();
    }

    public void updateThreadStatus(Long threadId, String status, LocalDateTime updatedAt) {
        jdbcTemplate.update("""
                UPDATE comment_report_threads
                SET status = ?, updated_at = ?
                WHERE id = ?
                """, status, Timestamp.valueOf(updatedAt), threadId);
    }

    public List<ResourceAppealMessageResponse> findMessagesByThreadId(Long threadId) {
        return jdbcTemplate.query("""
                SELECT id, sender_role, sender_name, content, created_at
                FROM comment_report_messages
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
                    INSERT INTO comment_report_messages
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
            throw new IllegalStateException("Failed to create comment report message.");
        }
        return key.longValue();
    }

    private CommentReportThreadRecord mapThread(ResultSet rs, int rowNum) throws SQLException {
        return new CommentReportThreadRecord(
                rs.getLong("id"),
                rs.getLong("comment_id"),
                rs.getLong("reporter_user_id"),
                rs.getString("reporter_name"),
                rs.getString("status"),
                toLocalDateTime(rs.getTimestamp("created_at")),
                toLocalDateTime(rs.getTimestamp("updated_at")),
                rs.getLong("resource_id"),
                rs.getString("comment_content"),
                rs.getString("comment_author"),
                rs.getString("resource_title"),
                rs.getString("resource_status")
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

    public record CommentReportThreadRecord(
            Long id,
            Long commentId,
            Long reporterUserId,
            String reporterName,
            String status,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            Long resourceId,
            String commentContent,
            String commentAuthor,
            String resourceTitle,
            String resourceStatus
    ) {
    }
}
