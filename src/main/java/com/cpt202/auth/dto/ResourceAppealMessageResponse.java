package com.cpt202.auth.dto;

/**
 * Appeal thread message item.
 */
public record ResourceAppealMessageResponse(
        Long id,
        String senderRole,
        String senderName,
        String content,
        String createdAt
) {
}
