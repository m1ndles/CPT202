package com.cpt202.auth.dto;

import java.util.List;

/**
 * Common page result.
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalItems,
        int totalPages
) {
}
