package com.cpt202.auth.dto;

/**
 * Appeal thread message item.
 *
 * @param id message id
 * @param senderRole sender role
 * @param senderName sender display name
 * @param content message content
 * @param createdAt message creation time
 */
public record ResourceAppealMessageResponse(
        Long id,
        String senderRole,
        String senderName,
        String content,
        String createdAt
) {
}
