package com.cpt202.auth.dto.admin;

/**
 * Managed category payload.
 *
 * @param id category id
 * @param name category name
 * @param description category description
 * @param status category status
 * @param updatedAt last update time
 * @param resourceCount number of related resources
 */
public record AdminCategoryItemResponse(
        Long id,
        String name,
        String description,
        String status,
        String updatedAt,
        int resourceCount
) {
}
