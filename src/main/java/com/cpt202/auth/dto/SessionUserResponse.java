package com.cpt202.auth.dto;

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
        String createdAt,
        String lastLoginAt
) {
}
