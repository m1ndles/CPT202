package com.cpt202.auth.dto;

import java.util.List;

/**
 * My Resources list item payload.
 *
 * @param id resource id
 * @param title resource title
 * @param category resource category
 * @param place resource place
 * @param description resource description
 * @param thumbnail thumbnail image url
 * @param trackingId submission tracking id
 * @param status resource status
 * @param createdAt creation time
 * @param tags resource tags
 */
public record MyResourceItemResponse(
        Long id,
        String title,
        String category,
        String place,
        String description,
        String thumbnail,
        String trackingId,
        String status,
        String createdAt,
        List<String> tags
) {
}
