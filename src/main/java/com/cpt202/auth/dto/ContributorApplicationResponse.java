package com.cpt202.auth.dto;

import java.util.List;

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
