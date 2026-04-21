package com.cpt202.auth.dto.admin;

/**
 * Archive list item payload.
 *
 * @param id archive id
 * @param resourceId archived resource id
 * @param title resource title
 * @param contributor contributor label
 * @param category resource category
 * @param archivedAt archive time
 * @param archivedBy archive operator
 * @param archiveReason archive reason
 * @param status archive status
 * @param publicationHistory publication history summary
 * @param originalMetadata original submission metadata
 */
public record AdminArchiveItemResponse(
        Long id,
        Long resourceId,
        String title,
        String contributor,
        String category,
        String archivedAt,
        String archivedBy,
        String archiveReason,
        String status,
        String publicationHistory,
        String originalMetadata
) {
}
