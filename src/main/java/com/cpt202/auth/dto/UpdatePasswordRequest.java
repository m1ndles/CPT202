package com.cpt202.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdatePasswordRequest(
        @NotBlank(message = "Current password is required.")
        String currentPassword,
        @NotBlank(message = "New password is required.")
        @Size(min = 8, message = "New password must be at least 8 characters.")
        String newPassword
) {
}
