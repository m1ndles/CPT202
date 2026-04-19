package com.cpt202.auth.dto.admin;

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
