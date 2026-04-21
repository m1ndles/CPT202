package com.cpt202.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Contributor application submission payload.
 *
 * @param fullName applicant full name
 * @param expertiseField selected expertise field
 * @param motivationStatement applicant motivation statement
 * @param portfolioLink optional portfolio link
 */
public record ContributorApplicationRequest(
        @NotBlank(message = "Full name is required.")
        @Size(max = 120, message = "Full name must be 120 characters or fewer.")
        String fullName,

        @NotBlank(message = "Expertise field is required.")
        @Size(max = 120, message = "Expertise field must be 120 characters or fewer.")
        String expertiseField,

        @NotBlank(message = "Motivation statement is required.")
        @Size(max = 2000, message = "Motivation statement must be 2000 characters or fewer.")
        String motivationStatement,

        @Size(max = 500, message = "Portfolio link must be 500 characters or fewer.")
        String portfolioLink
) {
}
