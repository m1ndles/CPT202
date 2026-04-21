package com.cpt202.auth.model;

import java.time.LocalDateTime;

/**
 * Comment data.
 *
 * @param id comment id
 * @param resourceId resource id that owns the comment
 * @param userId author id
 * @param content comment body
 * @param createdAt comment creation time
 */
public record Comment(
        Long id,
        Long resourceId,
        Long userId,
        String content,
        LocalDateTime createdAt
) {
}
