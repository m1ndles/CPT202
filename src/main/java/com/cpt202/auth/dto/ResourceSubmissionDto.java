package com.cpt202.auth.dto;

/**
 * Resource submission request payload.
 */
public record ResourceSubmissionDto(
        Long id,
        String title,
        String titleEn,
        String category,
        String period,
        String place,
        String tags,
        String description,
        String thumbnail,
        String copyright
) {
}
