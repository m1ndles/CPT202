package com.cpt202.auth.dto;

import java.util.List;

/**
 * Resource detail data.
 */
public record ResourceDetail(
        Long id,
        String title,
        String titleEn,
        String category,
        String place,
        String description,
        String thumbnail,
        String copyright,
        String status,
        int viewCount,
        String createdAt,
        List<String> tags,
        List<FileItem> files,
        List<LinkItem> links
) {
    /**
     * Resource file item.
     */
    public record FileItem(String name, String type, String url) {
    }

    /**
     * Resource link item.
     */
    public record LinkItem(String label, String url) {
    }
}
