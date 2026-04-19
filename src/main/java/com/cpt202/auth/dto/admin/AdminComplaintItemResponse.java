package com.cpt202.auth.dto.admin;

/**
 * Complaint inbox list item.
 */
public record AdminComplaintItemResponse(
        Long id,
        String complaintType,
        String title,
        String targetName,
        String status,
        String createdBy,
        String createdAt,
        String updatedAt,
        String latestMessagePreview,
        String actionLabel,
        String actionUrl
) {
}
