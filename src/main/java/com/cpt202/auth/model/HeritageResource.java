package com.cpt202.auth.model;

import java.time.LocalDateTime;

/**
 * Heritage resource data.
 *
 * @param id resource id
 * @param title primary resource title
 * @param titleEn English title variant
 * @param category resource category
 * @param period historical period
 * @param place related location
 * @param description resource description
 * @param thumbnail thumbnail image url
 * @param copyright copyright declaration
 * @param trackingId submission tracking id
 * @param status moderation status
 * @param viewCount number of views
 * @param createdAt creation time
 * @param ownerUserId owner user id
 * @param ownerUsername owner display name
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
