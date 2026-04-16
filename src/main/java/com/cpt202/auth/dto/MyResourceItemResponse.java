package com.cpt202.auth.dto;

import java.util.List;

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
