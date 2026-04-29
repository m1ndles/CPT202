package com.cpt202.auth.dto.admin;

/**
 * Complaint inbox list item.
 *
 * @param id complaint or thread id
 * @param complaintType complaint source type
 * @param title complaint display title
 * @param targetName reported or appealed target name
 * @param status complaint handling status
 * @param createdBy user who opened the thread
 * @param createdAt thread creation time
 * @param updatedAt latest thread update time
 * @param latestMessagePreview latest message preview
 * @param actionLabel label for the related target link
 * @param actionUrl related target link
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
