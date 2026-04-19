package com.cpt202.auth.dto.admin;

import java.util.List;

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
