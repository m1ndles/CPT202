package com.cpt202.auth.repository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

/**
 * Comment data access.
 */
@Repository
public class CommentRepository {

    private final JdbcTemplate jdbcTemplate;

    public CommentRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public long countByResourceId(Long resourceId) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) "
                        + "FROM comments c "
                        + "JOIN heritage_resources r ON r.id = c.resource_id "
                        + "WHERE c.resource_id = ? AND r.status = 'APPROVED'",
                Long.class,
                resourceId
        );
        return count == null ? 0 : count;
    }

    public List<CommentViewRow> findByResourceId(Long resourceId, Long viewerId, int offset, int limit) {
        return jdbcTemplate.query(
                "SELECT c.id, c.resource_id, c.user_id, c.content, c.created_at, u.username, u.role, "
                        + "COUNT(cl.user_id) AS likes, "
                        + "MAX(CASE WHEN cl.user_id = ? THEN 1 ELSE 0 END) AS liked_by_me "
                        + "FROM comments c "
                        + "JOIN heritage_resources r ON r.id = c.resource_id "
                        + "JOIN users u ON u.id = c.user_id "
                        + "LEFT JOIN comment_likes cl ON cl.comment_id = c.id "
                        + "WHERE c.resource_id = ? AND r.status = 'APPROVED' "
                        + "GROUP BY c.id, c.resource_id, c.user_id, c.content, c.created_at, u.username, u.role "
                        + "ORDER BY c.created_at DESC "
                        + "LIMIT ? OFFSET ?",
                viewRowMapper(),
                safeViewerId(viewerId),
                resourceId,
                limit,
                offset
        );
    }

    public Optional<CommentViewRow> findViewById(Long commentId, Long viewerId) {
        List<CommentViewRow> results = jdbcTemplate.query(
                "SELECT c.id, c.resource_id, c.user_id, c.content, c.created_at, u.username, u.role, "
                        + "COUNT(cl.user_id) AS likes, "
                        + "MAX(CASE WHEN cl.user_id = ? THEN 1 ELSE 0 END) AS liked_by_me "
                        + "FROM comments c "
                        + "JOIN heritage_resources r ON r.id = c.resource_id "
                        + "JOIN users u ON u.id = c.user_id "
                        + "LEFT JOIN comment_likes cl ON cl.comment_id = c.id "
                        + "WHERE c.id = ? AND r.status = 'APPROVED' "
                        + "GROUP BY c.id, c.resource_id, c.user_id, c.content, c.created_at, u.username, u.role",
                viewRowMapper(),
                safeViewerId(viewerId),
                commentId
        );
        return results.stream().findFirst();
    }

    public long create(Long resourceId, Long userId, String content) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO comments (resource_id, user_id, content) VALUES (?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS
            );
            statement.setLong(1, resourceId);
            statement.setLong(2, userId);
            statement.setString(3, content);
            return statement;
        }, keyHolder);
        Number key = keyHolder.getKey();
        return key == null ? 0 : key.longValue();
    }

    public boolean isLikedByUser(Long commentId, Long userId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM comment_likes WHERE comment_id = ? AND user_id = ?",
                Integer.class,
                commentId,
                userId
        );
        return count != null && count > 0;
    }

    public boolean addLike(Long commentId, Long userId) {
        int rows = jdbcTemplate.update(
                "INSERT IGNORE INTO comment_likes (comment_id, user_id) VALUES (?, ?)",
                commentId,
                userId
        );
        return rows > 0;
    }

    public void removeLike(Long commentId, Long userId) {
        jdbcTemplate.update(
                "DELETE FROM comment_likes WHERE comment_id = ? AND user_id = ?",
                commentId,
                userId
        );
    }

    private long safeViewerId(Long viewerId) {
        return viewerId == null ? -1L : viewerId;
    }

    private RowMapper<CommentViewRow> viewRowMapper() {
        return this::mapViewRow;
    }

    private CommentViewRow mapViewRow(ResultSet rs, int rowNum) throws SQLException {
        return new CommentViewRow(
                rs.getLong("id"),
                rs.getLong("resource_id"),
                rs.getLong("user_id"),
                rs.getString("username"),
                rs.getString("role"),
                rs.getString("content"),
                rs.getTimestamp("created_at") == null ? null : rs.getTimestamp("created_at").toLocalDateTime(),
                rs.getInt("likes"),
                rs.getInt("liked_by_me") > 0
        );
    }

    public record CommentViewRow(
            Long id,
            Long resourceId,
            Long userId,
            String username,
            String role,
            String content,
            LocalDateTime createdAt,
            int likes,
            boolean likedByMe
    ) {
    }
}
