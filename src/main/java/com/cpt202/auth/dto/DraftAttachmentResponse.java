package com.cpt202.auth.dto;

/**
 * Draft attachment response item.
 *
 * @param id attachment id
 * @param name original file name
 * @param type attachment type
 * @param url attachment access url
 */
public record DraftAttachmentResponse(
        Long id,
        String name,
        String type,
        String url
) {
}
