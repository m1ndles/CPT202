package com.cpt202.auth.dto.admin;

import jakarta.validation.constraints.NotBlank;

public record AdminTaxonomyRequest(
        @NotBlank(message = "Name is required.")
        String name,
        @NotBlank(message = "Description is required.")
        String description
) {
}
