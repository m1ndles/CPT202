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
 * Archive record data access.
 */
@Repository
public class AdminArchiveRepository {

    /**
     * JDBC helper used for archive queries and updates.
     */
    private final JdbcTemplate jdbcTemplate;

    public AdminArchiveRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Returns all archive records with resource details.
     */
    public List<ArchiveRecord> findAll() {
        return jdbcTemplate.query(
                """
                SELECT ar.id,
                       ar.resource_id,
                       r.title,
                       r.tracking_id,
                       ar.contributor_label,
                       r.category,
                       r.place,
                       r.description,
                       r.thumbnail,
                       ar.archived_at,
                       ar.archived_by,
                       ar.archive_reason,
                       ar.publication_history,
                       ar.original_metadata
                FROM admin_archive_records ar
                JOIN heritage_resources r ON r.id = ar.resource_id
                ORDER BY ar.archived_at DESC, ar.id DESC
                """,
                this::mapRecord
        );
    }

    /**
     * Returns a single archive record by id.
     */
    public Optional<ArchiveRecord> findById(Long id) {
        return jdbcTemplate.query(
                """
                SELECT ar.id,
                       ar.resource_id,
                       r.title,
                       r.tracking_id,
                       ar.contributor_label,
                       r.category,
                       r.place,
                       r.description,
                       r.thumbnail,
                       ar.archived_at,
                       ar.archived_by,
                       ar.archive_reason,
                       ar.publication_history,
                       ar.original_metadata
                FROM admin_archive_records ar
                JOIN heritage_resources r ON r.id = ar.resource_id
                WHERE ar.id = ?
                """,
                this::mapRecord,
                id
        ).stream().findFirst();
    }

    /**
     * Returns the archive record for a resource when present.
     */
    public Optional<ArchiveRecord> findByResourceId(Long resourceId) {
        return jdbcTemplate.query(
                """
                SELECT ar.id,
                       ar.resource_id,
                       r.title,
                       r.tracking_id,
                       ar.contributor_label,
                       r.category,
                       r.place,
                       r.description,
                       r.thumbnail,
                       ar.archived_at,
                       ar.archived_by,
                       ar.archive_reason,
                       ar.publication_history,
                       ar.original_metadata
                FROM admin_archive_records ar
                JOIN heritage_resources r ON r.id = ar.resource_id
                WHERE ar.resource_id = ?
                """,
                this::mapRecord,
                resourceId
        ).stream().findFirst();
    }

    /**
     * Returns the total number of archive records.
     */
    public long count() {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM admin_archive_records", Long.class);
        return count == null ? 0 : count;
    }

    /**
     * Creates or updates the archive record for a resource.
     */
    public Long upsert(Long resourceId,
                       String contributorLabel,
                       String archivedBy,
                       String archiveReason,
                       String publicationHistory,
                       String originalMetadata,
                       LocalDateTime archivedAt) {
        Optional<ArchiveRecord> existing = findByResourceId(resourceId);
        if (existing.isPresent()) {
            jdbcTemplate.update(
                    """
                    UPDATE admin_archive_records
                    SET contributor_label = ?, archived_by = ?, archive_reason = ?, publication_history = ?, original_metadata = ?, archived_at = ?
                    WHERE resource_id = ?
                    """,
                    contributorLabel,
                    archivedBy,
                    archiveReason,
                    publicationHistory,
                    originalMetadata,
                    Timestamp.valueOf(archivedAt),
                    resourceId
            );
            return existing.get().id();
        }

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            var statement = connection.prepareStatement(
                    """
                    INSERT INTO admin_archive_records
                    (resource_id, contributor_label, archived_by, archive_reason, publication_history, original_metadata, archived_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    """,
                    new String[]{"id"}
            );
            statement.setLong(1, resourceId);
            statement.setString(2, contributorLabel);
            statement.setString(3, archivedBy);
            statement.setString(4, archiveReason);
            statement.setString(5, publicationHistory);
            statement.setString(6, originalMetadata);
            statement.setTimestamp(7, Timestamp.valueOf(archivedAt));
            return statement;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("Failed to create archive record.");
        }
        return key.longValue();
    }

    /**
     * Deletes an archive record by id.
     */
    public void deleteById(Long id) {
        jdbcTemplate.update("DELETE FROM admin_archive_records WHERE id = ?", id);
    }

    /**
     * Deletes the archive record for a resource.
     */
    public void deleteByResourceId(Long resourceId) {
        jdbcTemplate.update("DELETE FROM admin_archive_records WHERE resource_id = ?", resourceId);
    }

    /**
     * Maps a result row into an archive record.
     */
    private ArchiveRecord mapRecord(ResultSet rs, int rowNum) throws SQLException {
        Timestamp archivedAt = rs.getTimestamp("archived_at");
        return new ArchiveRecord(
                rs.getLong("id"),
                rs.getLong("resource_id"),
                rs.getString("title"),
                rs.getString("tracking_id"),
                rs.getString("contributor_label"),
                rs.getString("category"),
                rs.getString("place"),
                rs.getString("description"),
                rs.getString("thumbnail"),
                archivedAt == null ? null : archivedAt.toLocalDateTime(),
                rs.getString("archived_by"),
                rs.getString("archive_reason"),
                rs.getString("publication_history"),
                rs.getString("original_metadata")
        );
    }

    /**
     * Immutable archive record view.
     *
     * @param id archive id
     * @param resourceId archived resource id
     * @param title archived resource title
     * @param contributorLabel contributor display label
     * @param category resource category
     * @param archivedAt archive time
     * @param archivedBy operator who archived the resource
     * @param archiveReason archive reason
     * @param publicationHistory publication history summary
     * @param originalMetadata original submission metadata
     */
    public record ArchiveRecord(
            Long id,
            Long resourceId,
            String title,
            String trackingId,
            String contributorLabel,
            String category,
            String place,
            String description,
            String thumbnailUrl,
            LocalDateTime archivedAt,
            String archivedBy,
            String archiveReason,
            String publicationHistory,
            String originalMetadata
    ) {
    }
}
