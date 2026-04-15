package com.cpt202.auth.repository;

import com.cpt202.auth.dto.ResourceDetail;
import com.cpt202.auth.model.HeritageResource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

/**
 * Resource data access.
 */
@Repository
public class ResourceRepository {

    private final JdbcTemplate jdbcTemplate;

    public ResourceRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public long countApproved(String keyword, String category, String place) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM heritage_resources r");
        List<Object> params = new ArrayList<>();
        appendFilters(sql, params, keyword, category, place);
        Long count = jdbcTemplate.queryForObject(sql.toString(), Long.class, params.toArray());
        return count == null ? 0 : count;
    }

    public List<HeritageResource> findApproved(String keyword, String category, String place,
                                               String sort, int offset, int limit) {
        StringBuilder sql = new StringBuilder("SELECT r.* FROM heritage_resources r");
        List<Object> params = new ArrayList<>();
        appendFilters(sql, params, keyword, category, place);
        sql.append(orderBy(sort));
        sql.append(" LIMIT ? OFFSET ?");
        params.add(limit);
        params.add(offset);
        return jdbcTemplate.query(sql.toString(), resourceRowMapper(), params.toArray());
    }

    public Optional<HeritageResource> findById(Long id) {
        List<HeritageResource> results = jdbcTemplate.query(
                "SELECT * FROM heritage_resources WHERE id = ? AND status = 'APPROVED'",
                resourceRowMapper(),
                id
        );
        return results.stream().findFirst();
    }

    public Optional<HeritageResource> findAnyById(Long id) {
        List<HeritageResource> results = jdbcTemplate.query(
                "SELECT * FROM heritage_resources WHERE id = ?",
                resourceRowMapper(),
                id
        );
        return results.stream().findFirst();
    }

    public List<HeritageResource> findAllResources() {
        return jdbcTemplate.query(
                "SELECT * FROM heritage_resources ORDER BY created_at DESC, id DESC",
                resourceRowMapper()
        );
    }

    public List<String> findCategories() {
        return jdbcTemplate.queryForList(
                "SELECT DISTINCT category FROM heritage_resources WHERE status = 'APPROVED' ORDER BY category",
                String.class
        );
    }

    public List<String> findPlaces() {
        return jdbcTemplate.queryForList(
                "SELECT DISTINCT place FROM heritage_resources WHERE status = 'APPROVED' ORDER BY place",
                String.class
        );
    }

    public List<String> findTagsByResourceId(Long resourceId) {
        return jdbcTemplate.queryForList(
                "SELECT tag FROM heritage_resource_tags WHERE resource_id = ? ORDER BY id",
                String.class,
                resourceId
        );
    }

    public List<ResourceDetail.FileItem> findFilesByResourceId(Long resourceId) {
        return jdbcTemplate.query(
                "SELECT name, type, url FROM heritage_resource_files WHERE resource_id = ? ORDER BY id",
                (rs, rowNum) -> new ResourceDetail.FileItem(
                        rs.getString("name"),
                        rs.getString("type"),
                        rs.getString("url")
                ),
                resourceId
        );
    }

    public List<ResourceDetail.LinkItem> findLinksByResourceId(Long resourceId) {
        return jdbcTemplate.query(
                "SELECT label, url FROM heritage_resource_links WHERE resource_id = ? ORDER BY id",
                (rs, rowNum) -> new ResourceDetail.LinkItem(
                        rs.getString("label"),
                        rs.getString("url")
                ),
                resourceId
        );
    }

    public void incrementViewCount(Long resourceId) {
        jdbcTemplate.update(
                "UPDATE heritage_resources SET view_count = view_count + 1 WHERE id = ? AND status = 'APPROVED'",
                resourceId
        );
    }

    public int getViewCount(Long resourceId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT view_count FROM heritage_resources WHERE id = ? AND status = 'APPROVED'",
                Integer.class,
                resourceId
        );
        return count == null ? 0 : count;
    }

    public HeritageResource insert(HeritageResource resource) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            var statement = connection.prepareStatement(
                    """
                    INSERT INTO heritage_resources
                    (title, title_en, category, period, place, description, thumbnail, copyright, tracking_id, status, view_count, created_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                    new String[]{"id"}
            );
            statement.setString(1, resource.title());
            statement.setString(2, resource.titleEn());
            statement.setString(3, resource.category());
            statement.setString(4, resource.period());
            statement.setString(5, resource.place());
            statement.setString(6, resource.description());
            statement.setString(7, resource.thumbnail());
            statement.setString(8, resource.copyright());
            statement.setString(9, resource.trackingId());
            statement.setString(10, resource.status());
            statement.setInt(11, resource.viewCount());
            statement.setTimestamp(12, resource.createdAt() == null ? null : Timestamp.valueOf(resource.createdAt()));
            return statement;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("Failed to create resource.");
        }

        return new HeritageResource(
                key.longValue(),
                resource.title(),
                resource.titleEn(),
                resource.category(),
                resource.period(),
                resource.place(),
                resource.description(),
                resource.thumbnail(),
                resource.copyright(),
                resource.trackingId(),
                resource.status(),
                resource.viewCount(),
                resource.createdAt()
        );
    }

    public HeritageResource updateDraft(HeritageResource resource) {
        jdbcTemplate.update(
                """
                UPDATE heritage_resources
                SET title = ?, title_en = ?, category = ?, period = ?, place = ?, description = ?, thumbnail = ?, copyright = ?, tracking_id = ?, status = ?, view_count = ?
                WHERE id = ?
                """,
                resource.title(),
                resource.titleEn(),
                resource.category(),
                resource.period(),
                resource.place(),
                resource.description(),
                resource.thumbnail(),
                resource.copyright(),
                resource.trackingId(),
                resource.status(),
                resource.viewCount(),
                resource.id()
        );
        return resource;
    }

    public Optional<HeritageResource> findDraftById(Long id) {
        List<HeritageResource> results = jdbcTemplate.query(
                "SELECT * FROM heritage_resources WHERE id = ? AND status = 'DRAFT'",
                resourceRowMapper(),
                id
        );
        return results.stream().findFirst();
    }

    public Long insertAttachment(Long resourceId, String name, String type, String url) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            var statement = connection.prepareStatement(
                    "INSERT INTO heritage_resource_files (resource_id, name, type, url) VALUES (?, ?, ?, ?)",
                    new String[]{"id"}
            );
            statement.setLong(1, resourceId);
            statement.setString(2, name);
            statement.setString(3, type);
            statement.setString(4, url);
            return statement;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("Failed to create attachment.");
        }
        return key.longValue();
    }

    public List<AttachmentRecord> findDraftAttachments(Long resourceId) {
        return jdbcTemplate.query(
                "SELECT id, name, type, url FROM heritage_resource_files WHERE resource_id = ? ORDER BY id",
                (rs, rowNum) -> new AttachmentRecord(
                        rs.getLong("id"),
                        rs.getString("name"),
                        rs.getString("type"),
                        rs.getString("url")
                ),
                resourceId
        );
    }

    public Optional<AttachmentRecord> findAttachmentById(Long resourceId, Long attachmentId) {
        List<AttachmentRecord> results = jdbcTemplate.query(
                "SELECT id, name, type, url FROM heritage_resource_files WHERE resource_id = ? AND id = ?",
                (rs, rowNum) -> new AttachmentRecord(
                        rs.getLong("id"),
                        rs.getString("name"),
                        rs.getString("type"),
                        rs.getString("url")
                ),
                resourceId,
                attachmentId
        );
        return results.stream().findFirst();
    }

    public void deleteAttachment(Long resourceId, Long attachmentId) {
        jdbcTemplate.update(
                "DELETE FROM heritage_resource_files WHERE resource_id = ? AND id = ?",
                resourceId,
                attachmentId
        );
    }

    public void replaceTags(Long resourceId, List<String> tags) {
        jdbcTemplate.update("DELETE FROM heritage_resource_tags WHERE resource_id = ?", resourceId);
        if (tags == null || tags.isEmpty()) {
            return;
        }
        for (String tag : tags) {
            jdbcTemplate.update(
                    "INSERT INTO heritage_resource_tags (resource_id, tag) VALUES (?, ?)",
                    resourceId,
                    tag
            );
        }
    }

    private void appendFilters(StringBuilder sql, List<Object> params,
                               String keyword, String category, String place) {
        sql.append(" WHERE r.status = 'APPROVED'");

        if (hasText(keyword)) {
            String like = "%" + escapeLike(keyword.trim().toLowerCase(Locale.ROOT)) + "%";
            sql.append(" AND (LOWER(r.title) LIKE ? OR LOWER(r.description) LIKE ? OR EXISTS (");
            sql.append("SELECT 1 FROM heritage_resource_tags t WHERE t.resource_id = r.id AND LOWER(t.tag) LIKE ?))");
            params.add(like);
            params.add(like);
            params.add(like);
        }

        if (hasText(category)) {
            sql.append(" AND r.category = ?");
            params.add(category.trim());
        }

        if (hasText(place)) {
            sql.append(" AND r.place = ?");
            params.add(place.trim());
        }
    }

    private String orderBy(String sort) {
        return switch (sort) {
            case "views" -> " ORDER BY r.view_count DESC";
            case "title" -> " ORDER BY r.title ASC";
            default -> " ORDER BY r.created_at DESC";
        };
    }

    private String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private RowMapper<HeritageResource> resourceRowMapper() {
        return this::mapResource;
    }

    private HeritageResource mapResource(ResultSet rs, int rowNum) throws SQLException {
        return new HeritageResource(
                rs.getLong("id"),
                rs.getString("title"),
                rs.getString("title_en"),
                rs.getString("category"),
                rs.getString("period"),
                rs.getString("place"),
                rs.getString("description"),
                rs.getString("thumbnail"),
                rs.getString("copyright"),
                rs.getString("tracking_id"),
                rs.getString("status"),
                rs.getInt("view_count"),
                rs.getTimestamp("created_at") == null ? null : rs.getTimestamp("created_at").toLocalDateTime()
        );
    }

    public record AttachmentRecord(Long id, String name, String type, String url) {
    }
}
