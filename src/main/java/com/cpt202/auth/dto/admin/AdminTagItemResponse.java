package com.cpt202.auth.dto.admin;

/**
 * Managed tag payload.
 *
 * @param id tag id
 * @param name tag name
 * @param description tag description
 * @param status tag status
 * @param updatedAt last update time
 * @param usageCount number of related resource tags
 */
public record AdminTagItemResponse(
        Long id,
        String name,
        String description,
        String status,
        String updatedAt,
        int usageCount
) {
}
