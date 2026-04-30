package com.cpt202.auth.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cpt202.auth.config.ContributorResourceDemoInitializer;
import com.cpt202.auth.config.DemoUserInitializer;
import com.cpt202.auth.config.HeritageDataInitializer;
import com.cpt202.auth.config.SchemaMigrationRunner;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * API-level regression coverage for threaded comments plus the complaint inbox report flow.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:comment_workflow_it;MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.sql.init.mode=always",
        "spring.sql.init.schema-locations=classpath:schema-comment-workflow-it.sql"
})
class CommentWorkflowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockBean
    private SchemaMigrationRunner schemaMigrationRunner;

    @MockBean
    private DemoUserInitializer demoUserInitializer;

    @MockBean
    private HeritageDataInitializer heritageDataInitializer;

    @MockBean
    private ContributorResourceDemoInitializer contributorResourceDemoInitializer;

    private MockHttpSession userSession;
    private MockHttpSession adminSession;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM comment_report_messages");
        jdbcTemplate.update("DELETE FROM comment_report_threads");
        jdbcTemplate.update("DELETE FROM comment_likes");
        jdbcTemplate.update("DELETE FROM comments");
        jdbcTemplate.update("DELETE FROM resource_report_messages");
        jdbcTemplate.update("DELETE FROM resource_report_threads");
        jdbcTemplate.update("DELETE FROM contributor_application_appeal_messages");
        jdbcTemplate.update("DELETE FROM contributor_applications");
        jdbcTemplate.update("DELETE FROM heritage_resources");
        jdbcTemplate.update("DELETE FROM users");

        jdbcTemplate.update("""
                INSERT INTO users (id, email, username, password_hash, role)
                VALUES (1, 'alice@example.com', 'alice', 'hash', 'USER')
                """);
        jdbcTemplate.update("""
                INSERT INTO users (id, email, username, password_hash, role)
                VALUES (2, 'admin@example.com', 'admin', 'hash', 'ADMIN')
                """);
        jdbcTemplate.update("""
                INSERT INTO heritage_resources (
                    id, title, title_en, category, period, place, description, thumbnail,
                    copyright, tracking_id, status, view_count, owner_user_id, owner_username
                )
                VALUES (
                    10, 'Kunqu Opera', 'Kunqu Opera', 'Performing Art', 'Ming Dynasty',
                    'Suzhou', 'A public heritage resource.', NULL, 'CC BY', 'TRK-10',
                    'APPROVED', 0, 1, 'alice'
                )
                """);

        userSession = session(1L, "alice", "USER");
        adminSession = session(2L, "admin", "ADMIN");
    }

    @Test
    void commentWorkflow_keepsReportAndAdminComplaintInboxWorking() throws Exception {
        long rootCommentId = idFrom(mockMvc.perform(post("/api/resources/10/comments")
                        .session(userSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("content", "Original root comment"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("Original root comment"))
                .andExpect(jsonPath("$.canReply").value(true))
                .andExpect(jsonPath("$.canEdit").value(true))
                .andReturn());

        long replyCommentId = idFrom(mockMvc.perform(post("/api/comments/{parentId}/reply", rootCommentId)
                        .session(userSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("content", "Second-level reply"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.parentId").value(rootCommentId))
                .andExpect(jsonPath("$.content").value("Second-level reply"))
                .andExpect(jsonPath("$.canReply").value(false))
                .andReturn());

        mockMvc.perform(put("/api/comments/{commentId}", rootCommentId)
                        .session(userSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("content", "Edited root comment"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("Edited root comment"))
                .andExpect(jsonPath("$.edited").value(true));

        mockMvc.perform(post("/api/comments/{commentId}/like", rootCommentId)
                        .session(userSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.likes").value(1))
                .andExpect(jsonPath("$.likedByMe").value(true));

        mockMvc.perform(post("/api/comments/{commentId}/reports", rootCommentId)
                        .session(userSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("content", "This comment needs moderator review."))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Comment report sent to the admin team."))
                .andExpect(jsonPath("$.messages[0].content").value("This comment needs moderator review."));

        MvcResult complaintsResult = mockMvc.perform(get("/api/admin/complaints")
                        .session(adminSession))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode complaints = objectMapper.readTree(body(complaintsResult));
        assertThat(complaints).anySatisfy(item -> {
            assertThat(item.get("complaintType").asText()).isEqualTo("COMMENT_REPORT");
            assertThat(item.get("targetName").asText()).isEqualTo("alice");
            assertThat(item.get("latestMessagePreview").asText()).contains("moderator review");
            assertThat(item.get("actionUrl").asText()).isEqualTo("/detail.html?id=10");
        });

        mockMvc.perform(delete("/api/comments/{commentId}", replyCommentId)
                        .session(userSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Comment deleted."));

        mockMvc.perform(get("/api/resources/10/comments?page=1&size=10")
                        .session(userSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(rootCommentId))
                .andExpect(jsonPath("$.content[0].content").value("Edited root comment"))
                .andExpect(jsonPath("$.content[0].replyCount").value(0));
    }

    private MockHttpSession session(Long userId, String username, String role) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("userId", userId);
        session.setAttribute("username", username);
        session.setAttribute("role", role);
        return session;
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    private long idFrom(MvcResult result) throws Exception {
        return objectMapper.readTree(body(result)).get("id").asLong();
    }

    private String body(MvcResult result) throws Exception {
        return result.getResponse().getContentAsString(StandardCharsets.UTF_8);
    }
}
