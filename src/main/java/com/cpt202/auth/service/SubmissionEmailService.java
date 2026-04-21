package com.cpt202.auth.service;

import com.cpt202.auth.model.HeritageResource;

/**
 * Contract for submission confirmation notifications.
 */
public interface SubmissionEmailService {

    /**
     * Sends a submission confirmation for the given resource.
     */
    void sendSubmissionConfirmation(String recipientEmail, HeritageResource resource);
}
