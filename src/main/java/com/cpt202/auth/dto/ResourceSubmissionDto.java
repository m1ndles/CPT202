package com.cpt202.auth.dto;

/**
 * Resource submission request payload.
 *
 * @param id resource id when updating an existing draft
 * @param title primary title
 * @param titleEn English title
 * @param category resource category
 * @param period historical period
 * @param place related place
 * @param tags comma-separated tags
 * @param description resource description
 * @param thumbnail thumbnail image url
 * @param copyright copyright declaration
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
