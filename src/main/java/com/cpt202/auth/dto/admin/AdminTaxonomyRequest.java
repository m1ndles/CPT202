package com.cpt202.auth.dto.admin;

import jakarta.validation.constraints.NotBlank;

/**
 * Category or tag update request payload.
 *
 * @param name taxonomy name
 * @param description taxonomy description
 */
public record AdminTaxonomyRequest(
        @NotBlank(message = "Name is required.")
        String name,
        @NotBlank(message = "Description is required.")
        String description
) {
}
