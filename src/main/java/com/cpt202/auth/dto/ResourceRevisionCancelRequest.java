package com.cpt202.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request used to cancel an in-progress revision draft.
 *
 * @param restoreStatus status restored when the revision is cancelled
 * @param title original resource title
 * @param titleEn original English title
 * @param category original resource category
 * @param period original historical period
 * @param place original related place
 * @param tags original comma-separated tags
 * @param description original resource description
 * @param thumbnail original thumbnail image url
 * @param copyright original copyright declaration
 */
public record ResourceRevisionCancelRequest(
        @NotBlank(message = "Restore status is required.")
        String restoreStatus,
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
