package com.cpt202.auth.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cpt202.auth.config.ContributorResourceDemoInitializer;
import com.cpt202.auth.config.DemoUserInitializer;
import com.cpt202.auth.config.HeritageDataInitializer;
import com.cpt202.auth.config.SchemaMigrationRunner;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Full cross-module integration tests for the authentication flow.
 *
 * <p>This test uses the real controller, service, repository, password encoder, and database.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:auth_flow_it;MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.sql.init.mode=always",
        "spring.sql.init.schema-locations=classpath:schema-auth-it.sql"
})
class AuthFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockBean
    private SchemaMigrationRunner schemaMigrationRunner;

    @MockBean
    private DemoUserInitializer demoUserInitializer;

    @MockBean
    private HeritageDataInitializer heritageDataInitializer;

    @MockBean
    private ContributorResourceDemoInitializer contributorResourceDemoInitializer;

    /**
     * Creates a real database user for the login flow.
     */
    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM users");
        jdbcTemplate.update("""
                        INSERT INTO users (email, username, password_hash, role, failed_attempts)
                        VALUES (?, ?, ?, ?, ?)
                        """,
                "integration.user@example.com",
                "integration_user",
                passwordEncoder.encode("Password123!"),
                "USER",
                3);
    }

    /**
     * Login flows through controller, service, repository, database, session, and cookie handling.
     */
    @Test
    void login_success_updatesDatabaseAndCreatesAuthenticatedSession() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "Integration.User@Example.com",
                                "password", "Password123!",
                                "rememberMe", false
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Login successful."))
                .andExpect(jsonPath("$.redirectUrl").value("/index.html"))
                .andExpect(jsonPath("$.username").value("integration_user"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(request().sessionAttribute("username", "integration_user"))
                .andExpect(request().sessionAttribute("email", "integration.user@example.com"))
                .andExpect(request().sessionAttribute("role", "USER"))
                .andExpect(cookie().exists("JSESSIONID"))
                .andExpect(cookie().httpOnly("JSESSIONID", true));

        Integer failedAttempts = jdbcTemplate.queryForObject(
                "SELECT failed_attempts FROM users WHERE email = ?",
                Integer.class,
                "integration.user@example.com");
        Integer loginTimestampCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE email = ? AND last_login_at IS NOT NULL",
                Integer.class,
                "integration.user@example.com");

        assertThat(failedAttempts).isZero();
        assertThat(loginTimestampCount).isEqualTo(1);
    }
}
