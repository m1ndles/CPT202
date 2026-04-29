package com.cpt202.auth.service;

import com.cpt202.auth.dto.ContributorEngagementResponse;
import com.cpt202.auth.dto.DailyMetricPoint;
import com.cpt202.auth.dto.SessionUserResponse;
import com.cpt202.auth.dto.UpdateEmailRequest;
import com.cpt202.auth.dto.UpdatePasswordRequest;
import com.cpt202.auth.dto.UpdateProfileRequest;
import com.cpt202.auth.exception.ApiException;
import com.cpt202.auth.model.UserAccount;
import com.cpt202.auth.model.UserRole;
import com.cpt202.auth.repository.ResourceRepository;
import com.cpt202.auth.repository.UserRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Profile management business logic.
 */
@Service
public class ProfileService {

    private static final long MAX_AVATAR_BYTES = 2L * 1024 * 1024;
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );
    private static final Path AVATAR_UPLOAD_DIR = Path.of("uploads", "avatars");
    private static final int DEFAULT_ENGAGEMENT_DAYS = 14;

    private final UserRepository userRepository;
    private final AuthService authService;
    private final PasswordEncoder passwordEncoder;
    private final ResourceRepository resourceRepository;

    public ProfileService(UserRepository userRepository,
                          AuthService authService,
                          PasswordEncoder passwordEncoder,
                          ResourceRepository resourceRepository) {
        this.userRepository = userRepository;
        this.authService = authService;
        this.passwordEncoder = passwordEncoder;
        this.resourceRepository = resourceRepository;
    }

    public SessionUserResponse getProfile(Long userId) {
        UserAccount user = requireUser(userId);
        return authService.getSessionUser(user.id(), user.role());
    }

    public SessionUserResponse updateProfile(Long userId, UpdateProfileRequest request) {
        UserAccount user = requireUser(userId);

        String normalizedUsername = request.username().trim();
        String normalizedBio = normalizeText(request.bio());
        String normalizedAvatarUrl = normalizeText(request.avatarUrl());

        if (userRepository.existsByUsernameExcludingUserId(normalizedUsername, user.id())) {
            throw new ApiException(HttpStatus.CONFLICT, "This username is already taken.");
        }

        userRepository.updateProfile(user.id(), normalizedUsername, normalizedBio, normalizedAvatarUrl);
        return authService.getSessionUser(user.id(), user.role());
    }

    public SessionUserResponse uploadAvatar(Long userId, MultipartFile file) {
        UserAccount user = requireUser(userId);

        if (file == null || file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Please choose an image file to upload.");
        }

        if (file.getSize() > MAX_AVATAR_BYTES) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Avatar image must be 2MB or smaller.");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Only JPG, PNG, and WEBP images are supported.");
        }

        String extension = switch (contentType.toLowerCase(Locale.ROOT)) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> "";
        };

        String fileName = "user-" + user.id() + "-" + UUID.randomUUID() + extension;
        Path target = AVATAR_UPLOAD_DIR.resolve(fileName).normalize();
        if (!target.startsWith(AVATAR_UPLOAD_DIR)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid upload path.");
        }

        try {
            Files.createDirectories(AVATAR_UPLOAD_DIR);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to store avatar image.");
        }

        String avatarUrl = "/uploads/avatars/" + fileName;
        userRepository.updateAvatarUrl(user.id(), avatarUrl);
        return authService.getSessionUser(user.id(), user.role());
    }

    public void updatePassword(Long userId, UpdatePasswordRequest request) {
        UserAccount user = requireUser(userId);

        if (!passwordEncoder.matches(request.currentPassword(), user.passwordHash())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Current password is incorrect.");
        }

        if (request.currentPassword().equals(request.newPassword())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "New password must be different from current password.");
        }

        String encoded = passwordEncoder.encode(request.newPassword());
        userRepository.updatePasswordHash(user.id(), encoded);
    }

    public SessionUserResponse updateEmail(Long userId, UpdateEmailRequest request) {
        UserAccount user = requireUser(userId);

        if (!passwordEncoder.matches(request.currentPassword(), user.passwordHash())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Current password is incorrect.");
        }

        String normalizedEmail = request.newEmail().trim().toLowerCase(Locale.ROOT);
        if (normalizedEmail.equals(user.email())) {
            return authService.getSessionUser(user.id(), user.role());
        }

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new ApiException(HttpStatus.CONFLICT, "An account with this email already exists.");
        }

        userRepository.updateEmail(user.id(), normalizedEmail);
        return authService.getSessionUser(user.id(), user.role());
    }

    /**
     * Returns favorite metrics received by the contributor's public resources.
     */
    public ContributorEngagementResponse getContributorEngagement(Long userId) {
        UserAccount user = requireUser(userId);
        if (user.role() != UserRole.CONTRIBUTOR && user.role() != UserRole.ADMIN) {
            return new ContributorEngagementResponse(0, java.util.List.of());
        }

        int totalReceivedLikes = resourceRepository.countFavoritesReceivedByOwner(user.id());
        java.util.List<DailyMetricPoint> trend = resourceRepository.findDailyFavoritesReceivedByOwner(
                user.id(),
                DEFAULT_ENGAGEMENT_DAYS
        );
        return new ContributorEngagementResponse(totalReceivedLikes, trend);
    }

    private UserAccount requireUser(Long userId) {
        if (userId == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Please log in to continue.");
        }

        return userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Please log in to continue."));
    }

    private String normalizeText(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
