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

@Repository
public class AdminTaxonomyRepository {

    private final JdbcTemplate jdbcTemplate;

    public AdminTaxonomyRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<TaxonomyRecord> findAllCategories() {
        return jdbcTemplate.query(
                "SELECT id, name, description, status, updated_at FROM admin_categories ORDER BY updated_at DESC, id DESC",
                this::mapRecord
        );
    }

    public List<TaxonomyRecord> findAllTags() {
        return jdbcTemplate.query(
                "SELECT id, name, description, status, updated_at FROM admin_tags ORDER BY updated_at DESC, id DESC",
                this::mapRecord
        );
    }

    public Optional<TaxonomyRecord> findCategoryById(Long id) {
        return jdbcTemplate.query(
                "SELECT id, name, description, status, updated_at FROM admin_categories WHERE id = ?",
                this::mapRecord,
                id
        ).stream().findFirst();
    }

    public Optional<TaxonomyRecord> findTagById(Long id) {
        return jdbcTemplate.query(
                "SELECT id, name, description, status, updated_at FROM admin_tags WHERE id = ?",
                this::mapRecord,
                id
        ).stream().findFirst();
    }

    public Optional<TaxonomyRecord> findCategoryByName(String name) {
        return jdbcTemplate.query(
                "SELECT id, name, description, status, updated_at FROM admin_categories WHERE LOWER(name) = LOWER(?)",
                this::mapRecord,
                name
        ).stream().findFirst();
    }

    public Optional<TaxonomyRecord> findTagByName(String name) {
        return jdbcTemplate.query(
                "SELECT id, name, description, status, updated_at FROM admin_tags WHERE LOWER(name) = LOWER(?)",
                this::mapRecord,
                name
        ).stream().findFirst();
    }

    public Long insertCategory(String name, String description, String status, LocalDateTime updatedAt) {
        return insert(
                "INSERT INTO admin_categories (name, description, status, updated_at) VALUES (?, ?, ?, ?)",
                name,
                description,
                status,
                updatedAt
        );
    }

    public Long insertTag(String name, String description, String status, LocalDateTime updatedAt) {
        return insert(
                "INSERT INTO admin_tags (name, description, status, updated_at) VALUES (?, ?, ?, ?)",
                name,
                description,
                status,
                updatedAt
        );
    }

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

    public void updateCategoryStatus(Long id, String status, LocalDateTime updatedAt) {
        jdbcTemplate.update(
                "UPDATE admin_categories SET status = ?, updated_at = ? WHERE id = ?",
                status,
                updatedAt,
                id
        );
    }

    public void updateTagStatus(Long id, String status, LocalDateTime updatedAt) {
        jdbcTemplate.update(
                "UPDATE admin_tags SET status = ?, updated_at = ? WHERE id = ?",
                status,
                updatedAt,
                id
        );
    }

    public long countCategories() {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM admin_categories", Long.class);
        return count == null ? 0 : count;
    }

    public long countTags() {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM admin_tags", Long.class);
        return count == null ? 0 : count;
    }

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

    public record TaxonomyRecord(
            Long id,
            String name,
            String description,
            String status,
            LocalDateTime updatedAt
    ) {
    }
}
