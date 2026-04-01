package com.cpt202.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "Email is required.")
        @Email(message = "Please enter a valid email address.")
        String email,
        @NotBlank(message = "Username is required.")
        @Size(min = 3, max = 60, message = "Username must be between 3 and 60 characters.")
        String username,
        @NotBlank(message = "Password is required.")
        @Size(min = 8, message = "Password must be at least 8 characters.")
        String password
) {
}
