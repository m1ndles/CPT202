package com.cpt202.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request for posting a comment.
 */
public record AddCommentRequest(
        @NotBlank(message = "Comment content is required.")
        @Size(max = 500, message = "Comment must not exceed 500 characters.")
        String content
) {
}
