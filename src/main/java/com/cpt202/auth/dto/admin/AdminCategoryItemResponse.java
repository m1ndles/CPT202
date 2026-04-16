package com.cpt202.auth.dto.admin;

public record AdminCategoryItemResponse(
        Long id,
        String name,
        String description,
        String status,
        String updatedAt,
        int resourceCount
) {
}
