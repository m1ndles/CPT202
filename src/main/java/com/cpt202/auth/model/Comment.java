package com.cpt202.auth.model;

import java.time.LocalDateTime;

/**
 * Comment data.
 */
public record Comment(
        Long id,
        Long resourceId,
        Long userId,
        String content,
        LocalDateTime createdAt
) {
}
