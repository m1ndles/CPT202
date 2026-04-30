package com.cpt202.auth.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Applies lightweight schema upgrades during startup.
 */
@Component
@Order(0)
public class SchemaMigrationRunner implements CommandLineRunner {

    /**
     * JDBC helper used to inspect and update the schema.
     */
    private final JdbcTemplate jdbcTemplate;

    public SchemaMigrationRunner(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Creates missing columns and tables required by the current codebase.
     */
    @Override
    public void run(String... args) {
        addColumnIfMissing(
                "users",
                "role",
                """
                ALTER TABLE users
                ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'USER'
                """
        );
        addColumnIfMissing(
                "users",
                "avatar_url",
                """
                ALTER TABLE users
                ADD COLUMN avatar_url VARCHAR(500) NULL
                """
        );
        addColumnIfMissing(
                "users",
                "bio",
                """
                ALTER TABLE users
                ADD COLUMN bio TEXT NULL
                """
        );
        addColumnIfMissing(
                "heritage_resources",
                "period",
                """
                ALTER TABLE heritage_resources
                ADD COLUMN period VARCHAR(100) NULL
                """
        );
        addColumnIfMissing(
                "heritage_resources",
                "tracking_id",
                """
                ALTER TABLE heritage_resources
                ADD COLUMN tracking_id VARCHAR(40) NULL UNIQUE
                """
        );
        addColumnIfMissing(
                "heritage_resources",
                "owner_user_id",
                """
                ALTER TABLE heritage_resources
                ADD COLUMN owner_user_id BIGINT NULL
                """
        );
        addColumnIfMissing(
                "heritage_resources",
                "owner_username",
                """
                ALTER TABLE heritage_resources
                ADD COLUMN owner_username VARCHAR(60) NULL
                """
        );
        createTableIfMissing(
                "resource_favorites",
                """
                CREATE TABLE resource_favorites (
                    resource_id BIGINT NOT NULL,
                    user_id BIGINT NOT NULL,
                    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    PRIMARY KEY (resource_id, user_id),
                    FOREIGN KEY (resource_id) REFERENCES heritage_resources(id) ON DELETE CASCADE,
                    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
                )
                """
        );
        createTableIfMissing(
                "resource_appeal_messages",
                """
                CREATE TABLE resource_appeal_messages (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    resource_id BIGINT NOT NULL,
                    sender_role VARCHAR(20) NOT NULL,
                    sender_name VARCHAR(120) NOT NULL,
                    content TEXT NOT NULL,
                    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (resource_id) REFERENCES heritage_resources(id) ON DELETE CASCADE
                )
                """
        );
        addColumnIfMissing(
                "comments",
                "parent_id",
                """
                ALTER TABLE comments
                ADD COLUMN parent_id BIGINT NULL
                """
        );
        addColumnIfMissing(
                "comments",
                "status",
                """
                ALTER TABLE comments
                ADD COLUMN status VARCHAR(10) NOT NULL DEFAULT 'ACTIVE'
                """
        );
        addColumnIfMissing(
                "comments",
                "updated_at",
                """
                ALTER TABLE comments
                ADD COLUMN updated_at DATETIME NULL
                """
        );
        createTableIfMissing(
                "comment_report_threads",
                """
                CREATE TABLE comment_report_threads (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    comment_id BIGINT NOT NULL,
                    reporter_user_id BIGINT NOT NULL,
                    reporter_name VARCHAR(120) NOT NULL,
                    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
                    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    UNIQUE KEY uk_comment_report_thread (comment_id, reporter_user_id),
                    FOREIGN KEY (comment_id) REFERENCES comments(id) ON DELETE CASCADE,
                    FOREIGN KEY (reporter_user_id) REFERENCES users(id) ON DELETE CASCADE
                )
                """
        );
        createTableIfMissing(
                "comment_report_messages",
                """
                CREATE TABLE comment_report_messages (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    thread_id BIGINT NOT NULL,
                    sender_role VARCHAR(20) NOT NULL,
                    sender_name VARCHAR(120) NOT NULL,
                    content TEXT NOT NULL,
                    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (thread_id) REFERENCES comment_report_threads(id) ON DELETE CASCADE
                )
                """
        );
        createTableIfMissing(
                "contributor_application_appeal_messages",
                """
                CREATE TABLE contributor_application_appeal_messages (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    application_id BIGINT NOT NULL,
                    sender_role VARCHAR(20) NOT NULL,
                    sender_name VARCHAR(120) NOT NULL,
                    content TEXT NOT NULL,
                    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (application_id) REFERENCES contributor_applications(id) ON DELETE CASCADE
                )
                """
        );
        createTableIfMissing(
                "resource_report_threads",
                """
                CREATE TABLE resource_report_threads (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    resource_id BIGINT NOT NULL,
                    reporter_user_id BIGINT NOT NULL,
                    reporter_name VARCHAR(120) NOT NULL,
                    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
                    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    UNIQUE KEY uk_resource_report_thread (resource_id, reporter_user_id),
                    FOREIGN KEY (resource_id) REFERENCES heritage_resources(id) ON DELETE CASCADE,
                    FOREIGN KEY (reporter_user_id) REFERENCES users(id) ON DELETE CASCADE
                )
                """
        );
        createTableIfMissing(
                "resource_report_messages",
                """
                CREATE TABLE resource_report_messages (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    thread_id BIGINT NOT NULL,
                    sender_role VARCHAR(20) NOT NULL,
                    sender_name VARCHAR(120) NOT NULL,
                    content TEXT NOT NULL,
                    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (thread_id) REFERENCES resource_report_threads(id) ON DELETE CASCADE
                )
                """
        );
    }

    /**
     * Adds a column only when it does not already exist.
     */
    private void addColumnIfMissing(String tableName, String columnName, String ddl) {
        Integer columnCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = ?
                  AND COLUMN_NAME = ?
                """, Integer.class, tableName, columnName);

        if (columnCount == null || columnCount == 0) {
            jdbcTemplate.execute(ddl);
        }
    }

    /**
     * Creates a table only when it does not already exist.
     */
    private void createTableIfMissing(String tableName, String ddl) {
        Integer tableCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.TABLES
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = ?
                """, Integer.class, tableName);

        if (tableCount == null || tableCount == 0) {
            jdbcTemplate.execute(ddl);
        }
    }
}
