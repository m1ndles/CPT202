package com.cpt202.auth.exception;

import org.springframework.http.HttpStatus;

/**
 * Runtime exception for API-level failures with an HTTP status.
 */
public class ApiException extends RuntimeException {

    /**
     * HTTP status returned for this exception.
     */
    private final HttpStatus status;

    /**
     * Creates a new API exception with a status and message.
     */
    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    /**
     * Returns the HTTP status associated with the failure.
     */
    public HttpStatus getStatus() {
        return status;
    }
}
