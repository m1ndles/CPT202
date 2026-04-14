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
}
