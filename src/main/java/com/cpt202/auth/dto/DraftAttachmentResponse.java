package com.cpt202.auth.dto;

/**
 * Draft attachment response item.
 */
public record DraftAttachmentResponse(
        Long id,
        String name,
        String type,
        String url
) {
}
