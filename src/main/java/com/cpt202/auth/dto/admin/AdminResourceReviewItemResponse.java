package com.cpt202.auth.dto.admin;

/**
 * Resource review queue item payload.
 *
 * @param id resource id
 * @param title resource title
 * @param contributor contributor label
 * @param category resource category
 * @param place related place
 * @param submissionDate submission date
 * @param status review status
 * @param thumbnailUrl thumbnail image url
 */
public record AdminResourceReviewItemResponse(
        Long id,
        String title,
        String contributor,
        String category,
        String place,
        String submissionDate,
        String status,
        String thumbnailUrl,
        int appealMessageCount,
        String latestAppealSenderRole,
        String latestAppealAt,
        String latestAppealPreview
) {
}
