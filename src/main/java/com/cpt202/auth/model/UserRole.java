package com.cpt202.auth.model;

public enum UserRole {
    GUEST("Guest", "Browse the platform with read-only access.", false, false, false),
    USER("Registered User", "Comment on resources and manage your personal profile.", true, false, false),
    CONTRIBUTOR("Contributor", "Registered member with contributor status shown on the profile.", true, true, false),
    ADMIN("Administrator", "Manage reviews, roles, categories, and publication workflow.", true, true, true);

    private final String label;
    private final String description;
    private final boolean canComment;
    private final boolean canUpload;
    private final boolean canAccessAdmin;

    UserRole(String label, String description, boolean canComment, boolean canUpload, boolean canAccessAdmin) {
        this.label = label;
        this.description = description;
        this.canComment = canComment;
        this.canUpload = canUpload;
        this.canAccessAdmin = canAccessAdmin;
    }

    public String label() {
        return label;
    }

    public String description() {
        return description;
    }

    public boolean canComment() {
        return canComment;
    }

    public boolean canUpload() {
        return canUpload;
    }

    public boolean canAccessAdmin() {
        return canAccessAdmin;
    }

    public String dashboardPath() {
        return this == ADMIN ? "/admin/dashboard.html" : "/index.html";
    }

    public static UserRole fromDatabaseValue(String value) {
        return UserRole.valueOf(value == null ? USER.name() : value.trim().toUpperCase());
    }
}
