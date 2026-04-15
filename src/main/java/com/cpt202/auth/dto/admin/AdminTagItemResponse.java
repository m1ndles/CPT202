package com.cpt202.auth.dto.admin;

public record AdminTagItemResponse(
        Long id,
        String name,
        String description,
        String status,
        String updatedAt,
        int usageCount
) {
}
