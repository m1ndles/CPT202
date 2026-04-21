package com.cpt202.auth.exception;

import org.springframework.http.HttpStatus;

/**
 * Runtime exception for authentication and authorization failures.
 */
public class AuthException extends RuntimeException {

    /**
     * HTTP status returned for this exception.
     */
    private final HttpStatus status;

    /**
     * Creates a new authentication exception with a status and message.
     */
    public AuthException(HttpStatus status, String message) {
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
