package com.cpt202.auth.model;

import java.time.LocalDateTime;

/**
 * User account data.
 *
 * @param id user id
 * @param email account email
 * @param username display username
 * @param passwordHash encoded password hash
 * @param role current user role
 * @param failedAttempts failed login attempts
 * @param lockedUntil lock expiration time
 * @param lastLoginAt last successful login time
 * @param avatarUrl avatar image url
 * @param bio profile biography
 * @param createdAt account creation time
 */
public record UserAccount(
        Long id,
        String email,
        String username,
        String passwordHash,
        UserRole role,
        int failedAttempts,
        LocalDateTime lockedUntil,
        LocalDateTime lastLoginAt,
        String avatarUrl,
        String bio,
        LocalDateTime createdAt
) {
}
