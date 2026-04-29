package com.cpt202.auth.dto.admin;

import com.cpt202.auth.dto.ResourceAppealMessageResponse;
import java.util.List;

/**
 * Complaint inbox detail payload.
 *
 * @param id complaint or thread id
 * @param complaintType complaint source type
 * @param title complaint display title
 * @param targetName reported or appealed target name
 * @param targetStatus current target status
 * @param status complaint handling status
 * @param createdBy user who opened the thread
 * @param createdAt thread creation time
 * @param updatedAt latest thread update time
 * @param helperText admin guidance for this complaint type
 * @param actionLabel label for the related target link
 * @param actionUrl related target link
 * @param canReply whether admin can reply in this thread
 * @param canReopenForReview whether admin can reopen the resource review
 * @param canDeleteComment whether admin can delete the reported comment
 * @param messages conversation messages in the thread
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
