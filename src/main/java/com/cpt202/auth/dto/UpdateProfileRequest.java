package com.cpt202.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Profile update request payload.
 *
 * @param username updated username
 * @param bio updated biography
 * @param avatarUrl avatar image url
 */
public record UpdateProfileRequest(
        @NotBlank(message = "Username is required.")
        @Size(min = 3, max = 60, message = "Username must be between 3 and 60 characters.")
        String username,
        @Size(max = 500, message = "Biography must be at most 500 characters.")
        String bio,
        @Size(max = 500, message = "Avatar URL must be at most 500 characters.")
        String avatarUrl
) {
}
