package com.cpt202.auth.dto.admin;

import jakarta.validation.constraints.NotBlank;

/**
 * Resource rejection request payload.
 *
 * @param rejectionComments rejection feedback text
 */
public record AdminRejectionRequest(
        @NotBlank(message = "Rejection comments are required.")
        String rejectionComments
) {
}
