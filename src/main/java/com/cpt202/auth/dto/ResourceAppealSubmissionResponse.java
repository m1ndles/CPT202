package com.cpt202.auth.dto;

import java.util.List;

/**
 * Appeal submission response payload.
 *
 * @param message result message
 * @param appealMessages updated appeal message list
 */
public record ResourceAppealSubmissionResponse(
        String message,
        List<ResourceAppealMessageResponse> appealMessages
) {
}
