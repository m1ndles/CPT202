package com.cpt202.auth.dto;

/**
 * Login result payload.
 *
 * @param message result message
 * @param redirectUrl next page url
 * @param username signed-in username
 * @param role signed-in role
 */
public record LoginResponse(
        String message,
        String redirectUrl,
        String username,
        String role
) {
}
