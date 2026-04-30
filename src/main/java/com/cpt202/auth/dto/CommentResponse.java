package com.cpt202.auth.dto;

/**
 * Comment view data.
 *
 * @param id comment id
 * @param parentId parent comment id
 * @param username author username
 * @param role author role label
 * @param content comment content
 * @param createdAt creation time
 * @param updatedAt last update time
 * @param edited whether the comment was edited
 * @param deleted whether the comment is deleted
 * @param likes like count
 * @param likedByMe whether the viewer liked the comment
 * @param canEdit whether the viewer can edit the comment
 * @param canDelete whether the current viewer can delete the comment
 * @param canReply whether the viewer can reply to the comment
 * @param replyCount reply count for root comments
 */
public record CommentResponse(
        Long id,
        Long parentId,
        String username,
        String role,
        String content,
        String createdAt,
        String updatedAt,
        boolean edited,
        boolean deleted,
        int likes,
        boolean likedByMe,
        boolean canEdit,
        boolean canDelete,
        boolean canReply,
        int replyCount
) {
}
