package com.cpt202.auth.dto;

import java.util.List;

/**
 * Generic thread submission response payload.
 */
public record MessageThreadSubmissionResponse(
        String message,
        List<ResourceAppealMessageResponse> messages
) {
}
