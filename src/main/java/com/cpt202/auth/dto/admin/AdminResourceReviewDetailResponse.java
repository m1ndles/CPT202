package com.cpt202.auth.dto.admin;

import com.cpt202.auth.dto.ResourceAppealMessageResponse;
import java.util.List;

public record AdminResourceReviewDetailResponse(
        Long id,
        String title,
        String subtitle,
        String description,
        String contributor,
        String category,
        String place,
        String submissionDate,
        String status,
        String thumbnailUrl,
        String imageUrl,
        String fileLink,
        String externalLink,
        List<String> tags,
        String copyrightDeclaration,
        String submissionMetadata,
        String rejectionComments,
        List<ResourceAppealMessageResponse> appealMessages,
        boolean canReplyInAppealThread,
        boolean visible
) {
}
