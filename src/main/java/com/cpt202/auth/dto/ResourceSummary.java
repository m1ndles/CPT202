package com.cpt202.auth.dto;

import java.util.List;

/**
 * Resource card data.
 */
public record ResourceSummary(
        Long id,
        String title,
        String category,
        String place,
        String thumbnail,
        List<String> tags,
        int viewCount,
        String createdAt
) {
}
