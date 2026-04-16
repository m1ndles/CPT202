package com.cpt202.auth.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Order(0)
public class SchemaMigrationRunner implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    public SchemaMigrationRunner(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

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
    }

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
