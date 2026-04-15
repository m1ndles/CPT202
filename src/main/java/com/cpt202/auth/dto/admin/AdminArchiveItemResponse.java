package com.cpt202.auth.dto.admin;

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
