package com.cpt202.auth.service;

import com.cpt202.auth.model.HeritageResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class LoggingSubmissionEmailService implements SubmissionEmailService {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoggingSubmissionEmailService.class);

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
