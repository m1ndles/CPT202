package com.cpt202.auth.dto;

import jakarta.validation.constraints.NotBlank;

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
