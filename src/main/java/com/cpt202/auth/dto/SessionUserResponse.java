package com.cpt202.auth.dto;

/**
 * Session user profile payload.
 *
 * @param username username shown in the session
 * @param email email shown in the session
 * @param role role code
 * @param roleLabel role display label
 * @param roleDescription role description
 * @param guest whether the session is a guest session
 * @param canComment whether the session can comment
 * @param canUpload whether the session can upload resources
 * @param canAccessAdmin whether the session can access admin pages
 * @param avatarUrl avatar image url
 * @param bio user biography
 * @param createdAt account creation time
 * @param lastLoginAt last successful login time
 */
public record SessionUserResponse(
        String username,
        String email,
        String role,
        String roleLabel,
        String roleDescription,
        boolean guest,
        boolean canComment,
        boolean canUpload,
        boolean canAccessAdmin,
        String avatarUrl,
        String bio,
        String createdAt,
        String lastLoginAt
) {
}
