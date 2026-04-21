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

    /**
     * JDBC helper used for comment queries and updates.
     */
    private final JdbcTemplate jdbcTemplate;

    public CommentRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Counts visible root comments for a resource.
     */
    public long countRootsByResourceId(Long resourceId) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) "
                        + "FROM comments c "
                        + "JOIN heritage_resources r ON r.id = c.resource_id "
                        + "WHERE c.resource_id = ? AND c.parent_id IS NULL AND r.status = 'APPROVED' "
                        + "AND (c.status = 'ACTIVE' OR EXISTS ("
                        + "    SELECT 1 FROM comments child WHERE child.parent_id = c.id AND child.status = 'ACTIVE'"
                        + "))",
                Long.class,
                resourceId
        );
        return count == null ? 0 : count;
    }

    /**
     * Returns paged root comments with viewer-specific like state.
     */
    public List<CommentViewRow> findRootsByResourceId(Long resourceId, Long viewerId, int offset, int limit) {
        return jdbcTemplate.query(
                "SELECT c.id, c.resource_id, c.user_id, c.parent_id, c.content, c.status, c.created_at, c.updated_at, u.username, u.role, "
                        + "COUNT(cl.user_id) AS likes, "
                        + "MAX(CASE WHEN cl.user_id = ? THEN 1 ELSE 0 END) AS liked_by_me "
                        + "FROM comments c "
                        + "JOIN heritage_resources r ON r.id = c.resource_id "
                        + "JOIN users u ON u.id = c.user_id "
                        + "LEFT JOIN comment_likes cl ON cl.comment_id = c.id "
                        + "WHERE c.resource_id = ? AND c.parent_id IS NULL AND r.status = 'APPROVED' "
                        + "AND (c.status = 'ACTIVE' OR EXISTS ("
                        + "    SELECT 1 FROM comments child WHERE child.parent_id = c.id AND child.status = 'ACTIVE'"
                        + ")) "
                        + "GROUP BY c.id, c.resource_id, c.user_id, c.parent_id, c.content, c.status, c.created_at, c.updated_at, u.username, u.role "
                        + "ORDER BY c.created_at DESC "
                        + "LIMIT ? OFFSET ?",
                viewRowMapper(),
                safeViewerId(viewerId),
                resourceId,
                limit,
                offset
        );
    }

    /**
     * Returns all active replies for the given root comments.
     */
    public List<CommentViewRow> findRepliesByParentIds(List<Long> parentIds, Long viewerId) {
        if (parentIds.isEmpty()) {
            return List.of();
        }
        String placeholders = String.join(",", java.util.Collections.nCopies(parentIds.size(), "?"));
        String sql = "SELECT c.id, c.resource_id, c.user_id, c.parent_id, c.content, c.status, c.created_at, c.updated_at, u.username, u.role, "
                + "COUNT(cl.user_id) AS likes, "
                + "MAX(CASE WHEN cl.user_id = ? THEN 1 ELSE 0 END) AS liked_by_me "
                + "FROM comments c "
                + "JOIN users u ON u.id = c.user_id "
                + "LEFT JOIN comment_likes cl ON cl.comment_id = c.id "
                + "WHERE c.parent_id IN (" + placeholders + ") AND c.status = 'ACTIVE' "
                + "GROUP BY c.id, c.resource_id, c.user_id, c.parent_id, c.content, c.status, c.created_at, c.updated_at, u.username, u.role "
                + "ORDER BY c.created_at ASC";
        Object[] args = new Object[parentIds.size() + 1];
        args[0] = safeViewerId(viewerId);
        for (int i = 0; i < parentIds.size(); i++) {
            args[i + 1] = parentIds.get(i);
        }
        return jdbcTemplate.query(sql, viewRowMapper(), args);
    }

    /**
     * Returns a single comment view row by id.
     */
    public Optional<CommentViewRow> findViewById(Long commentId, Long viewerId) {
        List<CommentViewRow> results = jdbcTemplate.query(
                "SELECT c.id, c.resource_id, c.user_id, c.parent_id, c.content, c.status, c.created_at, c.updated_at, u.username, u.role, "
                        + "COUNT(cl.user_id) AS likes, "
                        + "MAX(CASE WHEN cl.user_id = ? THEN 1 ELSE 0 END) AS liked_by_me "
                        + "FROM comments c "
                        + "JOIN heritage_resources r ON r.id = c.resource_id "
                        + "JOIN users u ON u.id = c.user_id "
                        + "LEFT JOIN comment_likes cl ON cl.comment_id = c.id "
                        + "WHERE c.id = ? AND r.status = 'APPROVED' "
                        + "GROUP BY c.id, c.resource_id, c.user_id, c.parent_id, c.content, c.status, c.created_at, c.updated_at, u.username, u.role",
                viewRowMapper(),
                safeViewerId(viewerId),
                commentId
        );
        return results.stream().findFirst();
    }

    /**
     * Inserts a new comment and returns its generated id.
     */
    public long create(Long resourceId, Long userId, Long parentId, String content) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO comments (resource_id, user_id, parent_id, content) VALUES (?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS
            );
            statement.setLong(1, resourceId);
            statement.setLong(2, userId);
            if (parentId == null) {
                statement.setNull(3, java.sql.Types.BIGINT);
            } else {
                statement.setLong(3, parentId);
            }
            statement.setString(4, content);
            return statement;
        }, keyHolder);
        Number key = keyHolder.getKey();
        return key == null ? 0 : key.longValue();
    }

    /**
     * Updates the text content of a comment.
     */
    public void updateContent(Long commentId, String content) {
        jdbcTemplate.update(
                "UPDATE comments SET content = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?",
                content,
                commentId
        );
    }

    /**
     * Marks a comment as deleted.
     */
    public void softDelete(Long commentId) {
        jdbcTemplate.update(
                "UPDATE comments SET status = 'DELETED', updated_at = CURRENT_TIMESTAMP WHERE id = ?",
                commentId
        );
    }

    /**
     * Counts active replies under a root comment.
     */
    public int countActiveRepliesByParentId(Long parentId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM comments WHERE parent_id = ? AND status = 'ACTIVE'",
                Integer.class,
                parentId
        );
        return count == null ? 0 : count;
    }

    /**
     * Returns whether the given user has liked the comment.
     */
    public boolean isLikedByUser(Long commentId, Long userId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM comment_likes WHERE comment_id = ? AND user_id = ?",
                Integer.class,
                commentId,
                userId
        );
        return count != null && count > 0;
    }

    /**
     * Adds a like to a comment when it does not already exist.
     */
    public boolean addLike(Long commentId, Long userId) {
        int rows = jdbcTemplate.update(
                "INSERT IGNORE INTO comment_likes (comment_id, user_id) VALUES (?, ?)",
                commentId,
                userId
        );
        return rows > 0;
    }

    /**
     * Removes a like from a comment.
     */
    public void removeLike(Long commentId, Long userId) {
        jdbcTemplate.update(
                "DELETE FROM comment_likes WHERE comment_id = ? AND user_id = ?",
                commentId,
                userId
        );
    }

    /**
     * Converts a nullable viewer id into a safe sentinel value.
     */
    private long safeViewerId(Long viewerId) {
        return viewerId == null ? -1L : viewerId;
    }

    /**
     * Returns the row mapper for comment view rows.
     */
    private RowMapper<CommentViewRow> viewRowMapper() {
        return this::mapViewRow;
    }

    /**
     * Maps a result row into a comment view projection.
     */
    private CommentViewRow mapViewRow(ResultSet rs, int rowNum) throws SQLException {
        long parentId = rs.getLong("parent_id");
        boolean parentNull = rs.wasNull();
        return new CommentViewRow(
                rs.getLong("id"),
                rs.getLong("resource_id"),
                rs.getLong("user_id"),
                parentNull ? null : parentId,
                rs.getString("username"),
                rs.getString("role"),
                rs.getString("content"),
                rs.getString("status"),
                rs.getTimestamp("created_at") == null ? null : rs.getTimestamp("created_at").toLocalDateTime(),
                rs.getTimestamp("updated_at") == null ? null : rs.getTimestamp("updated_at").toLocalDateTime(),
                rs.getInt("likes"),
                rs.getInt("liked_by_me") > 0
        );
    }

    /**
     * Comment view projection used by the service layer.
     *
     * @param id comment id
     * @param resourceId resource id
     * @param userId author id
     * @param parentId parent comment id
     * @param username author username
     * @param role author role
     * @param content comment body
     * @param status moderation status
     * @param createdAt creation time
     * @param updatedAt update time
     * @param likes like count
     * @param likedByMe whether the viewer liked the comment
     */
    public record CommentViewRow(
            Long id,
            Long resourceId,
            Long userId,
            Long parentId,
            String username,
            String role,
            String content,
            String status,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            int likes,
            boolean likedByMe
    ) {
    }
}
