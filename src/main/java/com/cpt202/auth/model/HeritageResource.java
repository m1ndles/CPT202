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
        String place,
        String description,
        String thumbnail,
        String copyright,
        String status,
        int viewCount,
        LocalDateTime createdAt
) {
}
