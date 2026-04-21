package com.cpt202.auth.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

/**
 * Category and tag taxonomy data access.
 */
@Repository
public class AdminTaxonomyRepository {

    /**
     * JDBC helper used for taxonomy queries and updates.
     */
    private final JdbcTemplate jdbcTemplate;

    public AdminTaxonomyRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Returns all managed categories.
     */
    public List<TaxonomyRecord> findAllCategories() {
        return jdbcTemplate.query(
                "SELECT id, name, description, status, updated_at FROM admin_categories ORDER BY updated_at DESC, id DESC",
                this::mapRecord
        );
    }

    /**
     * Returns all managed tags.
     */
    public List<TaxonomyRecord> findAllTags() {
        return jdbcTemplate.query(
                "SELECT id, name, description, status, updated_at FROM admin_tags ORDER BY updated_at DESC, id DESC",
                this::mapRecord
        );
    }

    /**
     * Returns a category by id.
     */
    public Optional<TaxonomyRecord> findCategoryById(Long id) {
        return jdbcTemplate.query(
                "SELECT id, name, description, status, updated_at FROM admin_categories WHERE id = ?",
                this::mapRecord,
                id
        ).stream().findFirst();
    }

    /**
     * Returns a tag by id.
     */
    public Optional<TaxonomyRecord> findTagById(Long id) {
        return jdbcTemplate.query(
                "SELECT id, name, description, status, updated_at FROM admin_tags WHERE id = ?",
                this::mapRecord,
                id
        ).stream().findFirst();
    }

    /**
     * Returns a category by name ignoring case.
     */
    public Optional<TaxonomyRecord> findCategoryByName(String name) {
        return jdbcTemplate.query(
                "SELECT id, name, description, status, updated_at FROM admin_categories WHERE LOWER(name) = LOWER(?)",
                this::mapRecord,
                name
        ).stream().findFirst();
    }

    /**
     * Returns a tag by name ignoring case.
     */
    public Optional<TaxonomyRecord> findTagByName(String name) {
        return jdbcTemplate.query(
                "SELECT id, name, description, status, updated_at FROM admin_tags WHERE LOWER(name) = LOWER(?)",
                this::mapRecord,
                name
        ).stream().findFirst();
    }

    /**
     * Inserts a new category row.
     */
    public Long insertCategory(String name, String description, String status, LocalDateTime updatedAt) {
        return insert(
                "INSERT INTO admin_categories (name, description, status, updated_at) VALUES (?, ?, ?, ?)",
                name,
                description,
                status,
                updatedAt
        );
    }

    /**
     * Inserts a new tag row.
     */
    public Long insertTag(String name, String description, String status, LocalDateTime updatedAt) {
        return insert(
                "INSERT INTO admin_tags (name, description, status, updated_at) VALUES (?, ?, ?, ?)",
                name,
                description,
                status,
                updatedAt
        );
    }

    /**
     * Updates a category row.
     */
    public void updateCategory(Long id, String name, String description, String status, LocalDateTime updatedAt) {
        jdbcTemplate.update(
                "UPDATE admin_categories SET name = ?, description = ?, status = ?, updated_at = ? WHERE id = ?",
                name,
                description,
                status,
                updatedAt,
                id
        );
    }

    /**
     * Updates a tag row.
     */
    public void updateTag(Long id, String name, String description, String status, LocalDateTime updatedAt) {
        jdbcTemplate.update(
                "UPDATE admin_tags SET name = ?, description = ?, status = ?, updated_at = ? WHERE id = ?",
                name,
                description,
                status,
                updatedAt,
                id
        );
    }

    /**
     * Updates only the status of a category.
     */
    public void updateCategoryStatus(Long id, String status, LocalDateTime updatedAt) {
        jdbcTemplate.update(
                "UPDATE admin_categories SET status = ?, updated_at = ? WHERE id = ?",
                status,
                updatedAt,
                id
        );
    }

    /**
     * Updates only the status of a tag.
     */
    public void updateTagStatus(Long id, String status, LocalDateTime updatedAt) {
        jdbcTemplate.update(
                "UPDATE admin_tags SET status = ?, updated_at = ? WHERE id = ?",
                status,
                updatedAt,
                id
        );
    }

    /**
     * Returns the total number of category rows.
     */
    public long countCategories() {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM admin_categories", Long.class);
        return count == null ? 0 : count;
    }

    /**
     * Returns the total number of tag rows.
     */
    public long countTags() {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM admin_tags", Long.class);
        return count == null ? 0 : count;
    }

    /**
     * Inserts a taxonomy row and returns its generated id.
     */
    private Long insert(String sql, String name, String description, String status, LocalDateTime updatedAt) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            var statement = connection.prepareStatement(sql, new String[]{"id"});
            statement.setString(1, name);
            statement.setString(2, description);
            statement.setString(3, status);
            statement.setTimestamp(4, Timestamp.valueOf(updatedAt));
            return statement;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("Failed to create taxonomy record.");
        }
        return key.longValue();
    }

    /**
     * Maps a result row into a taxonomy record.
     */
    private TaxonomyRecord mapRecord(ResultSet rs, int rowNum) throws SQLException {
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        return new TaxonomyRecord(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getString("description"),
                rs.getString("status"),
                updatedAt == null ? null : updatedAt.toLocalDateTime()
        );
    }

    /**
     * Immutable taxonomy row view.
     *
     * @param id taxonomy id
     * @param name taxonomy name
     * @param description taxonomy description
     * @param status active status
     * @param updatedAt last update time
     */
    public record TaxonomyRecord(
            Long id,
            String name,
            String description,
            String status,
            LocalDateTime updatedAt
    ) {
    }
}
