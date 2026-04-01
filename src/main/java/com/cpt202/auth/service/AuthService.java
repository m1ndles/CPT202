package com.cpt202.auth.service;

import com.cpt202.auth.dto.SessionUserResponse;
import com.cpt202.auth.exception.AuthException;
import com.cpt202.auth.model.UserAccount;
import com.cpt202.auth.model.UserRole;
import com.cpt202.auth.repository.UserRepository;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private static final int WARNING_THRESHOLD = 4;
    private static final int LOCK_THRESHOLD = 6;
    private static final int LOCK_MINUTES = 15;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public AuthenticatedUser login(String email, String password) {
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        UserAccount user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new AuthException(HttpStatus.UNAUTHORIZED, "Email or password is incorrect."));

        LocalDateTime now = LocalDateTime.now();
        if (user.lockedUntil() != null && user.lockedUntil().isAfter(now)) {
            long minutesLeft = Math.max(1, ChronoUnit.MINUTES.between(now, user.lockedUntil()) + 1);
            throw new AuthException(HttpStatus.LOCKED,
                    "Account locked. Please try again in " + minutesLeft + " minute(s).");
        }

        if (!passwordEncoder.matches(password, user.passwordHash())) {
            return handleFailedLogin(user, now);
        }

        userRepository.resetFailedLogin(user.id());
        return new AuthenticatedUser(user.id(), user.username(), user.email(), user.role());
    }

    public void register(String email, String username, String password) {
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        String normalizedUsername = username.trim();

        if (userRepository.existsByUsername(normalizedUsername)) {
            throw new AuthException(HttpStatus.CONFLICT, "This username is already taken.");
        }

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new AuthException(HttpStatus.CONFLICT, "An account with this email already exists. Please log in instead.");
        }

        userRepository.createUser(
                normalizedEmail,
                normalizedUsername,
                passwordEncoder.encode(password),
                UserRole.USER
        );
    }

    public SessionUserResponse getSessionUser(Long userId, UserRole sessionRole) {
        if (sessionRole == null) {
            throw new AuthException(HttpStatus.UNAUTHORIZED, "Please log in to continue.");
        }

        if (sessionRole == UserRole.GUEST) {
            return buildSessionUser(
                    "Guest Visitor",
                    "Guest session",
                    UserRole.GUEST,
                    true,
                    "Temporary access",
                    "Ends when you log out"
            );
        }

        if (userId == null) {
            throw new AuthException(HttpStatus.UNAUTHORIZED, "Please log in to continue.");
        }

        UserAccount user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException(HttpStatus.UNAUTHORIZED, "Please log in to continue."));

        return buildSessionUser(
                user.username(),
                user.email(),
                user.role(),
                false,
                formatDateTime(user.createdAt(), "Not available"),
                formatDateTime(user.lastLoginAt(), "This is your first completed login")
        );
    }

    private SessionUserResponse buildSessionUser(
            String username,
            String email,
            UserRole role,
            boolean guest,
            String createdAt,
            String lastLoginAt
    ) {
        return new SessionUserResponse(
                username,
                email,
                role.name(),
                role.label(),
                role.description(),
                guest,
                role.canComment(),
                role.canUpload(),
                role.canAccessAdmin(),
                createdAt,
                lastLoginAt
        );
    }

    private String formatDateTime(LocalDateTime value, String fallback) {
        return value == null ? fallback : DATE_TIME_FORMATTER.format(value);
    }

    private AuthenticatedUser handleFailedLogin(UserAccount user, LocalDateTime now) {
        int nextAttempt = user.failedAttempts() + 1;
        if (nextAttempt >= LOCK_THRESHOLD) {
            LocalDateTime lockedUntil = now.plusMinutes(LOCK_MINUTES);
            userRepository.updateFailedLogin(user.id(), nextAttempt, lockedUntil);
            throw new AuthException(HttpStatus.LOCKED,
                    "Account locked for 15 minutes due to multiple failed attempts.");
        }

        userRepository.updateFailedLogin(user.id(), nextAttempt, null);
        if (nextAttempt >= WARNING_THRESHOLD) {
            int attemptsLeft = LOCK_THRESHOLD - nextAttempt;
            throw new AuthException(HttpStatus.UNAUTHORIZED,
                    "Warning: Account will be locked after " + attemptsLeft + " more failed attempt(s).");
        }

        throw new AuthException(HttpStatus.UNAUTHORIZED, "Email or password is incorrect.");
    }

    public record AuthenticatedUser(Long id, String username, String email, UserRole role) {
    }
}
