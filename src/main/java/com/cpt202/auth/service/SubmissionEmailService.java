package com.cpt202.auth.service;

import com.cpt202.auth.model.HeritageResource;

public interface SubmissionEmailService {

    void sendSubmissionConfirmation(String recipientEmail, HeritageResource resource);
}
