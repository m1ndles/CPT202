package com.cpt202.auth.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cpt202.auth.exception.GlobalExceptionHandler;
import com.cpt202.auth.model.UserRole;
import com.cpt202.auth.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Integration tests for {@link AuthController} covering HTTP binding, validation, and session handling.
 */
@WebMvcTest(AuthController.class)
@Import(GlobalExceptionHandler.class)
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    /**
     * Successful login creates an HTTP session and returns the expected JSON payload.
     */
    @Test
    void login_success_createsSessionAndReturnsUserPayload() throws Exception {
        when(authService.login("alice@example.com", "correct-pw"))
                .thenReturn(new AuthService.AuthenticatedUser(
                        1L,
                        "alice",
                        "alice@example.com",
                        UserRole.USER
                ));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "alice@example.com",
                                "password", "correct-pw",
                                "rememberMe", true
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Login successful."))
                .andExpect(jsonPath("$.redirectUrl").value("/index.html"))
                .andExpect(jsonPath("$.username").value("alice"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(request().sessionAttribute("userId", 1L))
                .andExpect(request().sessionAttribute("username", "alice"))
                .andExpect(request().sessionAttribute("email", "alice@example.com"))
                .andExpect(request().sessionAttribute("role", "USER"))
                .andExpect(cookie().exists("JSESSIONID"))
                .andExpect(cookie().httpOnly("JSESSIONID", true))
                .andExpect(cookie().path("JSESSIONID", "/"))
                .andExpect(cookie().maxAge("JSESSIONID", 60 * 60 * 24 * 30));

        verify(authService).login("alice@example.com", "correct-pw");
    }

    /**
     * Invalid request payloads are rejected before reaching the service layer.
     */
    @Test
    void login_invalidEmail_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "not-an-email",
                                "password", "correct-pw",
                                "rememberMe", false
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Please enter a valid email address."));

        verifyNoInteractions(authService);
    }
}
