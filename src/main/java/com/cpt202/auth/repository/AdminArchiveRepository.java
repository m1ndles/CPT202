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
public class AdminArchiveRepository {

    private final JdbcTemplate jdbcTemplate;

    public AdminArchiveRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<ArchiveRecord> findAll() {
        return jdbcTemplate.query(
                """
                SELECT ar.id,
                       ar.resource_id,
                       r.title,
                       ar.contributor_label,
                       r.category,
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

    public Optional<ArchiveRecord> findById(Long id) {
        return jdbcTemplate.query(
                """
                SELECT ar.id,
                       ar.resource_id,
                       r.title,
                       ar.contributor_label,
                       r.category,
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

    public Optional<ArchiveRecord> findByResourceId(Long resourceId) {
        return jdbcTemplate.query(
                """
                SELECT ar.id,
                       ar.resource_id,
                       r.title,
                       ar.contributor_label,
                       r.category,
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

    public long count() {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM admin_archive_records", Long.class);
        return count == null ? 0 : count;
    }

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

    public void deleteById(Long id) {
        jdbcTemplate.update("DELETE FROM admin_archive_records WHERE id = ?", id);
    }

    public void deleteByResourceId(Long resourceId) {
        jdbcTemplate.update("DELETE FROM admin_archive_records WHERE resource_id = ?", resourceId);
    }

    private ArchiveRecord mapRecord(ResultSet rs, int rowNum) throws SQLException {
        Timestamp archivedAt = rs.getTimestamp("archived_at");
        return new ArchiveRecord(
                rs.getLong("id"),
                rs.getLong("resource_id"),
                rs.getString("title"),
                rs.getString("contributor_label"),
                rs.getString("category"),
                archivedAt == null ? null : archivedAt.toLocalDateTime(),
                rs.getString("archived_by"),
                rs.getString("archive_reason"),
                rs.getString("publication_history"),
                rs.getString("original_metadata")
        );
    }

    public record ArchiveRecord(
            Long id,
            Long resourceId,
            String title,
            String contributorLabel,
            String category,
            LocalDateTime archivedAt,
            String archivedBy,
            String archiveReason,
            String publicationHistory,
            String originalMetadata
    ) {
    }
}
