package com.cpt202.auth.dto.admin;

import com.cpt202.auth.dto.ResourceAppealMessageResponse;
import java.util.List;

/**
 * Resource review detail payload.
 *
 * @param id resource id
 * @param title primary title
 * @param subtitle secondary title
 * @param description resource description
 * @param contributor contributor label
 * @param category resource category
 * @param place related place
 * @param submissionDate submission date
 * @param status review status
 * @param thumbnailUrl thumbnail image url
 * @param imageUrl main image url
 * @param fileLink first file link
 * @param externalLink first external link
 * @param tags resource tags
 * @param copyrightDeclaration copyright declaration
 * @param submissionMetadata submission metadata summary
 * @param rejectionComments rejection feedback
 * @param appealMessages appeal thread messages
 * @param canReplyInAppealThread whether admins can reply in the appeal thread
 * @param visible whether the resource is publicly visible
 */
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
