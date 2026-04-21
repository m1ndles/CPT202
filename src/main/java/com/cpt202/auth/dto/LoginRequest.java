package com.cpt202.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Login request payload.
 *
 * @param email account email
 * @param password account password
 * @param rememberMe whether to extend the session lifetime
 */
public record LoginRequest(
        @NotBlank(message = "Email is required.")
        @Email(message = "Please enter a valid email address.")
        String email,
        @NotBlank(message = "Password is required.")
        String password,
        boolean rememberMe
) {
}
