package com.cpt202.auth.dto;

import java.util.List;

/**
 * Resource card data.
 *
 * @param id resource id
 * @param title resource title
 * @param category resource category
 * @param place related place
 * @param thumbnail thumbnail image url
 * @param tags preview tags
 * @param viewCount view count
 * @param createdAt creation time
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
