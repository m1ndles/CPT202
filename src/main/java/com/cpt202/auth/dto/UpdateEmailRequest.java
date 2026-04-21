package com.cpt202.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Email update request payload.
 *
 * @param currentPassword current password confirmation
 * @param newEmail new email address
 */
public record UpdateEmailRequest(
        @NotBlank(message = "Current password is required.")
        String currentPassword,
        @NotBlank(message = "Email is required.")
        @Email(message = "Please enter a valid email address.")
        String newEmail
) {
}
