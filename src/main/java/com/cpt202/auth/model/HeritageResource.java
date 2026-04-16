package com.cpt202.auth.model;

import java.time.LocalDateTime;

/**
 * Heritage resource data.
 */
public record HeritageResource(
        Long id,
        String title,
        String titleEn,
        String category,
        String period,
        String place,
        String description,
        String thumbnail,
        String copyright,
        String trackingId,
        String status,
        int viewCount,
        LocalDateTime createdAt,
        Long ownerUserId,
        String ownerUsername
) {
}
