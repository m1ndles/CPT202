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

@Repository
public class UserRepository {

    private final JdbcTemplate jdbcTemplate;

    public UserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<UserAccount> findByEmail(String email) {
        String sql = """
                SELECT id, email, username, password_hash, role, failed_attempts, locked_until, last_login_at, created_at
                FROM users
                WHERE email = ?
                """;

        List<UserAccount> results = jdbcTemplate.query(sql, userRowMapper(), email);
        return results.stream().findFirst();
    }

    public Optional<UserAccount> findById(Long userId) {
        String sql = """
                SELECT id, email, username, password_hash, role, failed_attempts, locked_until, last_login_at, created_at
                FROM users
                WHERE id = ?
                """;

        List<UserAccount> results = jdbcTemplate.query(sql, userRowMapper(), userId);
        return results.stream().findFirst();
    }

    public boolean existsByEmail(String email) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE email = ?",
                Integer.class,
                email
        );
        return count != null && count > 0;
    }

    public boolean existsByUsername(String username) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE username = ?",
                Integer.class,
                username
        );
        return count != null && count > 0;
    }

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

    public void resetFailedLogin(Long userId) {
        jdbcTemplate.update("""
                        UPDATE users
                        SET failed_attempts = 0, locked_until = NULL, last_login_at = CURRENT_TIMESTAMP
                        WHERE id = ?
                        """,
                userId);
    }

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

    private RowMapper<UserAccount> userRowMapper() {
        return this::mapUser;
    }

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
                rs.getTimestamp("created_at") == null ? null : rs.getTimestamp("created_at").toLocalDateTime()
        );
    }
}
