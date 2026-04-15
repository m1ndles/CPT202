package com.cpt202.auth.dto;

public record ContributorApplicationSummaryResponse(
        Long id,
        String username,
        String fullName,
        String expertiseField,
        String status,
        String submittedAt,
        String portfolioLink,
        String attachmentName,
        String attachmentUrl
) {
}
