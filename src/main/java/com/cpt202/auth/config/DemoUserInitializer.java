package com.cpt202.auth.config;

import com.cpt202.auth.model.UserRole;
import com.cpt202.auth.model.UserAccount;
import com.cpt202.auth.repository.UserRepository;
import java.util.Locale;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Order(1)
public class DemoUserInitializer implements CommandLineRunner {

    private static final String DEFAULT_USER_EMAIL = "demo@heritagehub.com";
    private static final String DEFAULT_CONTRIBUTOR_EMAIL = "contributor@heritagehub.com";
    private static final String TARGET_ADMIN_EMAIL = "admin@qq.com";
    private static final String LEGACY_ADMIN_EMAIL = "admin@heritagehub.com";
    private static final String ADMIN_USERNAME = "platform_admin";
    private static final String ADMIN_PASSWORD = "admin123456";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DemoUserInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        createUserIfMissing(DEFAULT_USER_EMAIL, "demo_user", "Password123!", UserRole.USER);
        createUserIfMissing(DEFAULT_CONTRIBUTOR_EMAIL, "archive_builder", "Password123!", UserRole.CONTRIBUTOR);
        syncAdminAccount();
    }

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

    private void syncAdminAccount() {
        String normalizedTargetEmail = TARGET_ADMIN_EMAIL.toLowerCase(Locale.ROOT);
        String passwordHash = passwordEncoder.encode(ADMIN_PASSWORD);

        userRepository.findByEmail(normalizedTargetEmail)
                .ifPresentOrElse(
                        user -> updateAdminUser(user, normalizedTargetEmail, passwordHash),
                        () -> migrateLegacyOrCreateAdmin(normalizedTargetEmail, passwordHash)
                );
    }

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
