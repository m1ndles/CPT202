package com.cpt202.auth.dto;

/**
 * Comment view data.
 *
 * @param id comment id
 * @param username comment author username
 * @param role comment author role label
 * @param content comment body
 * @param createdAt creation time
 * @param likes current like count
 * @param likedByMe whether the current viewer liked the comment
 * @param canDelete whether the current viewer can delete the comment
 */
public record CommentResponse(
        Long id,
        String username,
        String role,
        String content,
        String createdAt,
        int likes,
        boolean likedByMe,
        boolean canDelete
) {
}
