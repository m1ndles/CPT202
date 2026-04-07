package com.cpt202.auth.config;

import javax.sql.DataSource;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;

/**
 * Seed heritage data on first startup.
 */
@Component
@Order(2)
public class HeritageDataInitializer implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;
    private final DataSource dataSource;

    public HeritageDataInitializer(JdbcTemplate jdbcTemplate, DataSource dataSource) {
        this.jdbcTemplate = jdbcTemplate;
        this.dataSource = dataSource;
    }

    @Override
    public void run(String... args) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM heritage_resources", Integer.class);
        if (count != null && count > 0) {
            return;
        }

        FileSystemResource script = new FileSystemResource("sql/seed-heritage-data.sql");
        if (!script.exists()) {
            return;
        }

        ResourceDatabasePopulator populator = new ResourceDatabasePopulator(false, false, "UTF-8", script);
        populator.execute(dataSource);
    }
}
