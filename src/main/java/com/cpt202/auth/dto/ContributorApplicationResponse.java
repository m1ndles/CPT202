package com.cpt202.auth.dto;

/**
 * Contributor application detail payload.
 *
 * @param id application id
 * @param userId applicant user id
 * @param username applicant username
 * @param email applicant email
 * @param fullName applicant full name
 * @param expertiseField expertise field
 * @param motivationStatement motivation statement
 * @param portfolioLink portfolio link
 * @param status review status
 * @param rejectionComments rejection comments
 * @param submittedAt submission time
 * @param reviewedAt review time
 * @param attachmentName uploaded attachment name
 * @param attachmentUrl uploaded attachment url
 */
public record ContributorApplicationResponse(
        Long id,
        Long userId,
        String username,
        String email,
        String fullName,
        String expertiseField,
        String motivationStatement,
        String portfolioLink,
        String status,
        String rejectionComments,
        String submittedAt,
        String reviewedAt,
        String attachmentName,
        String attachmentUrl
) {
}
