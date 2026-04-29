package com.cpt202.auth.dto.admin;

import java.util.List;

/**
 * Archive management item and detail payload.
 *
 * @param id archive record id
 * @param resourceId archived resource id
 * @param title archived resource title
 * @param contributor contributor display label
 * @param category resource category
 * @param place resource place
 * @param trackingId submission tracking id
 * @param tags archived resource tags
 * @param fileName first attached file name
 * @param fileLink first attached file url
 * @param externalLabel first external reference label
 * @param externalLink first external reference url
 * @param description archived resource description
 * @param thumbnailUrl thumbnail image url
 * @param archivedAt archive time
 * @param archivedBy administrator who archived the resource
 * @param archiveReason archive reason
 * @param status archive lifecycle status
 * @param publicationHistory publication history summary
 * @param originalMetadata original submission metadata
 */
public record AdminArchiveItemResponse(
        Long id,
        Long resourceId,
        String title,
        String contributor,
        String category,
        String place,
        String trackingId,
        List<String> tags,
        String fileName,
        String fileLink,
        String externalLabel,
        String externalLink,
        String description,
        String thumbnailUrl,
        String archivedAt,
        String archivedBy,
        String archiveReason,
        String status,
        String publicationHistory,
        String originalMetadata
) {
}
