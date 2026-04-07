package com.cpt202.auth.dto;

/**
 * Comment view data.
 */
public record CommentResponse(
        Long id,
        String username,
        String role,
        String content,
        String createdAt,
        int likes,
        boolean likedByMe
) {
}
