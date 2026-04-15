package com.cpt202.auth.dto.admin;

import jakarta.validation.constraints.NotBlank;

public record AdminRejectionRequest(
        @NotBlank(message = "Rejection comments are required.")
        String rejectionComments
) {
}
