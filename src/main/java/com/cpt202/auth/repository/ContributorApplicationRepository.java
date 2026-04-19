package com.cpt202.auth.repository;

import com.cpt202.auth.dto.ContributorApplicationResponse;
import com.cpt202.auth.dto.ContributorApplicationSummaryResponse;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ContributorApplicationRepository {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final JdbcTemplate jdbcTemplate;

    public ContributorApplicationRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<ContributorApplicationResponse> findLatestByUserId(Long userId) {
        String sql = """
                SELECT ca.id,
                       ca.user_id,
                       u.username,
                       u.email,
                       ca.full_name,
                       ca.expertise_field,
                       ca.motivation_statement,
                       ca.portfolio_link,
                       ca.status,
                       ca.rejection_comments,
                       ca.submitted_at,
                       ca.reviewed_at,
                       ca.attachment_name,
                       ca.attachment_url
                FROM contributor_applications ca
                JOIN users u ON u.id = ca.user_id
                WHERE ca.user_id = ?
                ORDER BY ca.submitted_at DESC, ca.id DESC
                LIMIT 1
                """;
        List<ContributorApplicationResponse> items = jdbcTemplate.query(sql, this::mapDetail, userId);
        return items.stream().findFirst();
    }

    public List<ContributorApplicationSummaryResponse> findByUserId(Long userId) {
        String sql = """
                SELECT ca.id,
                       u.username,
                       ca.full_name,
                       ca.expertise_field,
                       ca.status,
                       ca.submitted_at,
                       ca.portfolio_link,
                       ca.attachment_name,
                       ca.attachment_url
                FROM contributor_applications ca
                JOIN users u ON u.id = ca.user_id
                WHERE ca.user_id = ?
                ORDER BY ca.submitted_at DESC, ca.id DESC
                """;
        return jdbcTemplate.query(sql, this::mapSummary, userId);
    }

    public List<ContributorApplicationSummaryResponse> findAllPending() {
        String sql = """
                SELECT ca.id,
                       u.username,
                       ca.full_name,
                       ca.expertise_field,
                       ca.status,
                       ca.submitted_at,
                       ca.portfolio_link,
                       ca.attachment_name,
                       ca.attachment_url
                FROM contributor_applications ca
                JOIN users u ON u.id = ca.user_id
                WHERE ca.status = 'PENDING'
                ORDER BY ca.submitted_at ASC, ca.id ASC
                """;
        return jdbcTemplate.query(sql, this::mapSummary);
    }

    public List<ContributorApplicationSummaryResponse> findAllApplications() {
        String sql = """
                SELECT ca.id,
                       u.username,
                       ca.full_name,
                       ca.expertise_field,
                       ca.status,
                       ca.submitted_at,
                       ca.portfolio_link,
                       ca.attachment_name,
                       ca.attachment_url
                FROM contributor_applications ca
                JOIN users u ON u.id = ca.user_id
                ORDER BY ca.submitted_at DESC, ca.id DESC
                """;
        return jdbcTemplate.query(sql, this::mapSummary);
    }

    public Optional<ContributorApplicationResponse> findById(Long id) {
        String sql = """
                SELECT ca.id,
                       ca.user_id,
                       u.username,
                       u.email,
                       ca.full_name,
                       ca.expertise_field,
                       ca.motivation_statement,
                       ca.portfolio_link,
                       ca.status,
                       ca.rejection_comments,
                       ca.submitted_at,
                       ca.reviewed_at,
                       ca.attachment_name,
                       ca.attachment_url
                FROM contributor_applications ca
                JOIN users u ON u.id = ca.user_id
                WHERE ca.id = ?
                """;
        List<ContributorApplicationResponse> items = jdbcTemplate.query(sql, this::mapDetail, id);
        return items.stream().findFirst();
    }

    public Long insert(Long userId,
                       String fullName,
                       String expertiseField,
                       String motivationStatement,
                       String portfolioLink,
                       String attachmentName,
                       String attachmentUrl) {
        jdbcTemplate.update("""
                        INSERT INTO contributor_applications (
                            user_id, full_name, expertise_field, motivation_statement, portfolio_link,
                            attachment_name, attachment_url, status
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, 'PENDING')
                        """,
                userId,
                fullName,
                expertiseField,
                motivationStatement,
                portfolioLink,
                attachmentName,
                attachmentUrl
        );

        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    public void updateReview(Long applicationId, String status, String rejectionComments) {
        jdbcTemplate.update("""
                        UPDATE contributor_applications
                        SET status = ?, rejection_comments = ?, reviewed_at = CURRENT_TIMESTAMP
                        WHERE id = ?
                        """,
                status,
                rejectionComments,
                applicationId
        );
    }

    private ContributorApplicationSummaryResponse mapSummary(ResultSet rs, int rowNum) throws SQLException {
        return new ContributorApplicationSummaryResponse(
                rs.getLong("id"),
                rs.getString("username"),
                rs.getString("full_name"),
                rs.getString("expertise_field"),
                rs.getString("status"),
                format(rs.getTimestamp("submitted_at")),
                rs.getString("portfolio_link"),
                rs.getString("attachment_name"),
                rs.getString("attachment_url")
        );
    }

    private ContributorApplicationResponse mapDetail(ResultSet rs, int rowNum) throws SQLException {
        return new ContributorApplicationResponse(
                rs.getLong("id"),
                rs.getLong("user_id"),
                rs.getString("username"),
                rs.getString("email"),
                rs.getString("full_name"),
                rs.getString("expertise_field"),
                rs.getString("motivation_statement"),
                rs.getString("portfolio_link"),
                rs.getString("status"),
                rs.getString("rejection_comments"),
                format(rs.getTimestamp("submitted_at")),
                format(rs.getTimestamp("reviewed_at")),
                rs.getString("attachment_name"),
                rs.getString("attachment_url"),
                List.of(),
                false
        );
    }

    private String format(Timestamp timestamp) {
        if (timestamp == null) {
            return null;
        }
        LocalDateTime value = timestamp.toLocalDateTime();
        return DATE_TIME_FORMATTER.format(value);
    }
}
