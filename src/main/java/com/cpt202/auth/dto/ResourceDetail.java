package com.cpt202.auth.dto;

import java.util.List;

/**
 * Resource detail data.
 *
 * @param id resource id
 * @param title primary title
 * @param titleEn English title
 * @param category resource category
 * @param period historical period
 * @param place related place
 * @param description resource description
 * @param thumbnail thumbnail image url
 * @param copyright copyright declaration
 * @param trackingId submission tracking id
 * @param status moderation status
 * @param viewCount view count
 * @param favoriteCount favorite count
 * @param favoritedByMe whether the viewer favorited the resource
 * @param createdAt creation time
 * @param rejectionComments latest rejection feedback
 * @param appealMessages appeal thread messages
 * @param canSendAppeal whether the viewer can send a new appeal message
 * @param reportMessages resource report messages visible to the current viewer
 * @param canSendReport whether the current viewer can report the resource
 * @param tags resource tags
 * @param files attached files
 * @param links external links
 */
public record ResourceDetail(
        Long id,
        String title,
        String titleEn,
        String category,
        String period,
        String place,
        String description,
        String thumbnail,
        String copyright,
        String trackingId,
        String status,
        int viewCount,
        int favoriteCount,
        boolean favoritedByMe,
        String createdAt,
        String rejectionComments,
        List<ResourceAppealMessageResponse> appealMessages,
        boolean canSendAppeal,
        List<ResourceAppealMessageResponse> reportMessages,
        boolean canSendReport,
        List<String> tags,
        List<FileItem> files,
        List<LinkItem> links
) {
    /**
     * Resource file item.
     *
     * @param name file name
     * @param type file type
     * @param url file access url
     */
    public record FileItem(String name, String type, String url) {
    }

    /**
     * Resource link item.
     *
     * @param label link label
     * @param url link target url
     */
    public record LinkItem(String label, String url) {
    }
}
