package com.cpt202.auth.dto;

import java.util.List;

/**
 * Contributor application detail payload.
 *
 * @param id application id
 * @param userId applicant user id
 * @param username applicant username
 * @param email applicant email
 * @param fullName applicant full name
 * @param expertiseField selected expertise field
 * @param motivationStatement motivation statement
 * @param portfolioLink portfolio or supporting link
 * @param status review status
 * @param rejectionComments latest admin rejection comments
 * @param submittedAt submission time
 * @param reviewedAt review time
 * @param attachmentName uploaded supporting file name
 * @param attachmentUrl uploaded supporting file url
 * @param appealMessages contributor application appeal messages
 * @param canSendAppeal whether the applicant can send appeal messages
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
        String attachmentUrl,
        List<ResourceAppealMessageResponse> appealMessages,
        boolean canSendAppeal
) {
}
