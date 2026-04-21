package com.cpt202.auth.model;

/**
 * Supported user roles and their platform permissions.
 */
public enum UserRole {
    GUEST("Guest", "Browse the platform with read-only access.", false, false, false),
    USER("Registered User", "Comment on resources and manage your personal profile.", true, false, false),
    CONTRIBUTOR("Contributor", "Registered member with contributor status shown on the profile.", true, true, false),
    ADMIN("Administrator", "Manage reviews, roles, categories, and publication workflow.", true, true, true);

    /**
     * Human-readable role label.
     */
    private final String label;

    /**
     * Short description for the role.
     */
    private final String description;

    /**
     * Whether the role can post comments.
     */
    private final boolean canComment;

    /**
     * Whether the role can submit resources.
     */
    private final boolean canUpload;

    /**
     * Whether the role can enter the admin console.
     */
    private final boolean canAccessAdmin;

    UserRole(String label, String description, boolean canComment, boolean canUpload, boolean canAccessAdmin) {
        this.label = label;
        this.description = description;
        this.canComment = canComment;
        this.canUpload = canUpload;
        this.canAccessAdmin = canAccessAdmin;
    }

    /**
     * Returns the display label for the role.
     */
    public String label() {
        return label;
    }

    /**
     * Returns the summary description for the role.
     */
    public String description() {
        return description;
    }

    /**
     * Returns whether the role can comment.
     */
    public boolean canComment() {
        return canComment;
    }

    /**
     * Returns whether the role can upload resources.
     */
    public boolean canUpload() {
        return canUpload;
    }

    /**
     * Returns whether the role can access the admin console.
     */
    public boolean canAccessAdmin() {
        return canAccessAdmin;
    }

    /**
     * Returns the default landing page for the role.
     */
    public String dashboardPath() {
        return this == ADMIN ? "/admin/dashboard.html" : "/index.html";
    }

    /**
     * Parses a database value into a role.
     */
    public static UserRole fromDatabaseValue(String value) {
        return UserRole.valueOf(value == null ? USER.name() : value.trim().toUpperCase());
    }
}
