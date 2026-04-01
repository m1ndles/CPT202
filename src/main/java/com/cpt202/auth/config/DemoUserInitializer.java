package com.cpt202.auth.config;

import com.cpt202.auth.model.UserRole;
import com.cpt202.auth.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Order(1)
public class DemoUserInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DemoUserInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        createUserIfMissing("demo@heritagehub.com", "demo_user", "Password123!", UserRole.USER);
        createUserIfMissing("contributor@heritagehub.com", "archive_builder", "Password123!", UserRole.CONTRIBUTOR);
        createUserIfMissing("admin@heritagehub.com", "platform_admin", "AdminPass123!", UserRole.ADMIN);
    }

    private void createUserIfMissing(String email, String username, String rawPassword, UserRole role) {
        if (!userRepository.existsByEmail(email)) {
            userRepository.createUser(
                    email,
                    username,
                    passwordEncoder.encode(rawPassword),
                    role
            );
        }
    }
}
