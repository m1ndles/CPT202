package com.cpt202.auth.dto;

public record LoginResponse(
        String message,
        String redirectUrl,
        String username,
        String role
) {
}
