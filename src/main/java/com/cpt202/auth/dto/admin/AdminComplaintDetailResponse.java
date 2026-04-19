package com.cpt202.auth.dto.admin;

import com.cpt202.auth.dto.ResourceAppealMessageResponse;
import java.util.List;

/**
 * Complaint inbox detail payload.
 */
public record AdminComplaintDetailResponse(
        Long id,
        String complaintType,
        String title,
        String targetName,
        String targetStatus,
        String status,
        String createdBy,
        String createdAt,
        String updatedAt,
        String helperText,
        String actionLabel,
        String actionUrl,
        boolean canReply,
        boolean canReopenForReview,
        boolean canDeleteComment,
        List<ResourceAppealMessageResponse> messages
) {
}
