package com.cpt202.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Appeal submission request.
 */
public record ResourceAppealRequest(
        @NotBlank(message = "Appeal message content is required.")
        @Size(max = 1000, message = "Appeal message must be 1000 characters or fewer.")
        String content
) {
}
