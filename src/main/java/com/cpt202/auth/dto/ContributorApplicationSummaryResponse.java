package com.cpt202.auth.dto;

/**
 * Contributor application summary payload.
 *
 * @param id application id
 * @param username applicant username
 * @param fullName applicant full name
 * @param expertiseField expertise field
 * @param status review status
 * @param submittedAt submission time
 * @param portfolioLink portfolio link
 * @param attachmentName uploaded attachment name
 * @param attachmentUrl uploaded attachment url
 */
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
