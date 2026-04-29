package com.cpt202.auth.dto;

import java.util.List;

/**
 * Generic thread submission response payload.
 *
 * @param message operation result message
 * @param messages refreshed conversation messages after submission
 */
public record MessageThreadSubmissionResponse(
        String message,
        List<ResourceAppealMessageResponse> messages
) {
}
