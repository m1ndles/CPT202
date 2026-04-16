package com.cpt202.auth.dto;

import java.util.List;

/**
 * Appeal submission response payload.
 */
public record ResourceAppealSubmissionResponse(
        String message,
        List<ResourceAppealMessageResponse> appealMessages
) {
}
