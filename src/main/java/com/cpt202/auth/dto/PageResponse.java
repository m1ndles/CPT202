package com.cpt202.auth.dto;

import java.util.List;

/**
 * Common page result.
 *
 * @param <T> content item type
 * @param content page content
 * @param page current page number
 * @param size page size
 * @param totalItems total item count
 * @param totalPages total page count
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalItems,
        int totalPages
) {
}
