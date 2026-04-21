package com.cpt202.auth.config;

import com.cpt202.auth.model.UserRole;
import com.cpt202.auth.model.UserAccount;
import com.cpt202.auth.repository.UserRepository;
import java.util.Locale;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Seeds the default demo accounts used by the application.
 */
@Component
@Order(1)
public class DemoUserInitializer implements CommandLineRunner {

    /**
     * Default registered user account email.
     */
    private static final String DEFAULT_USER_EMAIL = "demo@heritagehub.com";

    /**
     * Default contributor account email.
     */
    private static final String DEFAULT_CONTRIBUTOR_EMAIL = "contributor@heritagehub.com";

    /**
     * Target administrator email used by the current demo data.
     */
    private static final String TARGET_ADMIN_EMAIL = "admin@qq.com";

    /**
     * Legacy administrator email kept for migration.
     */
    private static final String LEGACY_ADMIN_EMAIL = "admin@heritagehub.com";

    /**
     * Default administrator username.
     */
    private static final String ADMIN_USERNAME = "platform_admin";

    /**
     * Default administrator password.
     */
    private static final String ADMIN_PASSWORD = "admin123456";

    /**
     * Repository used to create and update demo users.
     */
    private final UserRepository userRepository;

    /**
     * Password encoder used for demo passwords.
     */
    private final PasswordEncoder passwordEncoder;

    public DemoUserInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Seeds the demo user, contributor, and administrator accounts.
     */
    @Override
    public void run(String... args) {
        createUserIfMissing(DEFAULT_USER_EMAIL, "demo_user", "Password123!", UserRole.USER);
        createUserIfMissing(DEFAULT_CONTRIBUTOR_EMAIL, "archive_builder", "Password123!", UserRole.CONTRIBUTOR);
        syncAdminAccount();
    }

    /**
     * Creates a demo account when the email does not already exist.
     */
    private void createUserIfMissing(String email, String username, String rawPassword, UserRole role) {
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        if (!userRepository.existsByEmail(normalizedEmail)) {
            userRepository.createUser(
                    normalizedEmail,
                    username,
                    passwordEncoder.encode(rawPassword),
                    role
            );
        }
    }

    /**
     * Ensures the administrator account exists with the expected credentials.
     */
    private void syncAdminAccount() {
        String normalizedTargetEmail = TARGET_ADMIN_EMAIL.toLowerCase(Locale.ROOT);
        String passwordHash = passwordEncoder.encode(ADMIN_PASSWORD);

        userRepository.findByEmail(normalizedTargetEmail)
                .ifPresentOrElse(
                        user -> updateAdminUser(user, normalizedTargetEmail, passwordHash),
                        () -> migrateLegacyOrCreateAdmin(normalizedTargetEmail, passwordHash)
                );
    }

    /**
     * Migrates the legacy admin account or creates a new one.
     */
    private void migrateLegacyOrCreateAdmin(String normalizedTargetEmail, String passwordHash) {
        userRepository.findByEmail(LEGACY_ADMIN_EMAIL)
                .ifPresentOrElse(
                        user -> updateAdminUser(user, normalizedTargetEmail, passwordHash),
                        () -> userRepository.createUser(
                                normalizedTargetEmail,
                                ADMIN_USERNAME,
                                passwordHash,
                                UserRole.ADMIN
                        )
                );
    }

    /**
     * Updates an existing account to the expected administrator profile.
     */
    private void updateAdminUser(UserAccount user, String normalizedTargetEmail, String passwordHash) {
        userRepository.updateUser(
                user.id(),
                normalizedTargetEmail,
                ADMIN_USERNAME,
                passwordHash,
                UserRole.ADMIN
        );
    }
}
