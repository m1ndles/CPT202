package com.cpt202.auth.repository;

import com.cpt202.auth.model.UserAccount;
import com.cpt202.auth.model.UserRole;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/**
 * User account data access.
 */
@Repository
public class UserRepository {

    /**
     * JDBC helper used for user queries and updates.
     */
    private final JdbcTemplate jdbcTemplate;

    public UserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Returns a user by email address.
     */
    public Optional<UserAccount> findByEmail(String email) {
        String sql = """
                SELECT id, email, username, password_hash, role, failed_attempts, locked_until, last_login_at, avatar_url, bio, created_at
                FROM users
                WHERE email = ?
                """;

        List<UserAccount> results = jdbcTemplate.query(sql, userRowMapper(), email);
        return results.stream().findFirst();
    }

    /**
     * Returns a user by id.
     */
    public Optional<UserAccount> findById(Long userId) {
        String sql = """
                SELECT id, email, username, password_hash, role, failed_attempts, locked_until, last_login_at, avatar_url, bio, created_at
                FROM users
                WHERE id = ?
                """;

        List<UserAccount> results = jdbcTemplate.query(sql, userRowMapper(), userId);
        return results.stream().findFirst();
    }

    /**
     * Returns whether a user already exists for the email address.
     */
    public boolean existsByEmail(String email) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE email = ?",
                Integer.class,
                email
        );
        return count != null && count > 0;
    }

    /**
     * Returns whether a username is already in use.
     */
    public boolean existsByUsername(String username) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE username = ?",
                Integer.class,
                username
        );
        return count != null && count > 0;
    }

    /**
     * Returns whether a username is used by another account.
     */
    public boolean existsByUsernameExcludingUserId(String username, Long userId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE username = ? AND id <> ?",
                Integer.class,
                username,
                userId
        );
        return count != null && count > 0;
    }

    /**
     * Updates failed login counters and the optional lock time.
     */
    public void updateFailedLogin(Long userId, int failedAttempts, LocalDateTime lockedUntil) {
        jdbcTemplate.update("""
                        UPDATE users
                        SET failed_attempts = ?, locked_until = ?
                        WHERE id = ?
                        """,
                failedAttempts,
                lockedUntil,
                userId);
    }

    /**
     * Resets failed login counters after a successful login.
     */
    public void resetFailedLogin(Long userId) {
        jdbcTemplate.update("""
                        UPDATE users
                        SET failed_attempts = 0, locked_until = NULL, last_login_at = CURRENT_TIMESTAMP
                        WHERE id = ?
                        """,
                userId);
    }

    /**
     * Inserts a new user account.
     */
    public void createUser(String email, String username, String passwordHash, UserRole role) {
        jdbcTemplate.update("""
                        INSERT INTO users (email, username, password_hash, role)
                        VALUES (?, ?, ?, ?)
                        """,
                email,
                username,
                passwordHash,
                role.name());
    }

    /**
     * Updates the main identity fields for a user.
     */
    public void updateUser(Long userId, String email, String username, String passwordHash, UserRole role) {
        jdbcTemplate.update("""
                        UPDATE users
                        SET email = ?, username = ?, password_hash = ?, role = ?
                        WHERE id = ?
                        """,
                email,
                username,
                passwordHash,
                role.name(),
                userId);
    }

    /**
     * Updates the profile fields for a user.
     */
    public void updateProfile(Long userId, String username, String bio, String avatarUrl) {
        jdbcTemplate.update("""
                        UPDATE users
                        SET username = ?, bio = ?, avatar_url = ?
                        WHERE id = ?
                        """,
                username,
                bio,
                avatarUrl,
                userId);
    }

    /**
     * Updates the avatar url for a user.
     */
    public void updateAvatarUrl(Long userId, String avatarUrl) {
        jdbcTemplate.update("""
                        UPDATE users
                        SET avatar_url = ?
                        WHERE id = ?
                        """,
                avatarUrl,
                userId);
    }

    /**
     * Updates the password hash for a user.
     */
    public void updatePasswordHash(Long userId, String passwordHash) {
        jdbcTemplate.update("""
                        UPDATE users
                        SET password_hash = ?
                        WHERE id = ?
                        """,
                passwordHash,
                userId);
    }

    /**
     * Updates the email address for a user.
     */
    public void updateEmail(Long userId, String email) {
        jdbcTemplate.update("""
                        UPDATE users
                        SET email = ?
                        WHERE id = ?
                        """,
                email,
                userId);
    }

    /**
     * Returns the row mapper for user accounts.
     */
    private RowMapper<UserAccount> userRowMapper() {
        return this::mapUser;
    }

    /**
     * Maps a result row into a user account.
     */
    private UserAccount mapUser(ResultSet rs, int rowNum) throws SQLException {
        return new UserAccount(
                rs.getLong("id"),
                rs.getString("email"),
                rs.getString("username"),
                rs.getString("password_hash"),
                UserRole.fromDatabaseValue(rs.getString("role")),
                rs.getInt("failed_attempts"),
                rs.getTimestamp("locked_until") == null ? null : rs.getTimestamp("locked_until").toLocalDateTime(),
                rs.getTimestamp("last_login_at") == null ? null : rs.getTimestamp("last_login_at").toLocalDateTime(),
                rs.getString("avatar_url"),
                rs.getString("bio"),
                rs.getTimestamp("created_at") == null ? null : rs.getTimestamp("created_at").toLocalDateTime()
        );
    }
}
