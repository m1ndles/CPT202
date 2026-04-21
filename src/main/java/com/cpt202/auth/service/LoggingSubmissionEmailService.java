package com.cpt202.auth.service;

import com.cpt202.auth.model.HeritageResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Logs submission confirmation events instead of sending real emails.
 */
@Service
public class LoggingSubmissionEmailService implements SubmissionEmailService {

    /**
     * Logger used for the email placeholder output.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(LoggingSubmissionEmailService.class);

    /**
     * Writes the submission confirmation payload to the application log.
     */
    @Override
    public void sendSubmissionConfirmation(String recipientEmail, HeritageResource resource) {
        LOGGER.info(
                "Submission confirmation email placeholder -> to={}, resourceId={}, trackingId={}, status={}",
                recipientEmail,
                resource.id(),
                resource.trackingId(),
                resource.status()
        );
    }
}
