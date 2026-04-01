package com.cpt202.auth.model;

import java.time.LocalDateTime;

public record UserAccount(
        Long id,
        String email,
        String username,
        String passwordHash,
        UserRole role,
        int failedAttempts,
        LocalDateTime lockedUntil,
        LocalDateTime lastLoginAt,
        LocalDateTime createdAt
) {
}
