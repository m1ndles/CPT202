package com.cpt202.auth.service;

import com.cpt202.auth.dto.ContributorApplicationRequest;
import com.cpt202.auth.dto.ContributorApplicationResponse;
import com.cpt202.auth.dto.ContributorApplicationSummaryResponse;
import com.cpt202.auth.exception.ApiException;
import com.cpt202.auth.model.UserAccount;
import com.cpt202.auth.model.UserRole;
import com.cpt202.auth.repository.AdminActivityRepository;
import com.cpt202.auth.repository.ContributorApplicationRepository;
import com.cpt202.auth.repository.ResourceRepository;
import com.cpt202.auth.repository.UserRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Contributor application business logic.
 */
@Service
public class ContributorApplicationService {

    /**
     * Maximum supporting file size in bytes.
     */
    private static final long MAX_FILE_BYTES = 10L * 1024 * 1024;

    /**
     * Supported supporting file content types.
     */
    private static final Set<String> ALLOWED_FILE_TYPES = Set.of(
            "application/pdf",
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    /**
     * Storage directory for contributor application files.
     */
    private static final Path APPLICATION_UPLOAD_DIR = Path.of("uploads", "applications");

    /**
     * Repository used to read and update contributor applications.
     */
    private final ContributorApplicationRepository contributorApplicationRepository;

    /**
     * Repository used to validate expertise fields against resource categories.
     */
    private final ResourceRepository resourceRepository;

    /**
     * Repository used to load and update user accounts.
     */
    private final UserRepository userRepository;

    /**
     * Repository used to record administrator actions.
     */
    private final AdminActivityRepository adminActivityRepository;

    public ContributorApplicationService(ContributorApplicationRepository contributorApplicationRepository,
                                         ResourceRepository resourceRepository,
                                         UserRepository userRepository,
                                         AdminActivityRepository adminActivityRepository) {
        this.contributorApplicationRepository = contributorApplicationRepository;
        this.resourceRepository = resourceRepository;
        this.userRepository = userRepository;
        this.adminActivityRepository = adminActivityRepository;
    }

    /**
     * Returns the latest application for the current user.
     */
    public ContributorApplicationResponse getCurrentApplication(Long userId) {
        requireEligibleUser(userId);
        return contributorApplicationRepository.findLatestByUserId(userId).orElse(null);
    }

    /**
     * Returns the full application history for the current user.
     */
    public List<ContributorApplicationSummaryResponse> getMyApplications(Long userId) {
        requireSignedInUser(userId);
        return contributorApplicationRepository.findByUserId(userId);
    }

    /**
     * Submits a contributor application and stores its optional file.
     */
    @Transactional
    public ContributorApplicationResponse submit(Long userId,
                                                 ContributorApplicationRequest request,
                                                 MultipartFile file) {
        UserAccount user = requireEligibleUser(userId);
        ContributorApplicationResponse latest = contributorApplicationRepository.findLatestByUserId(user.id()).orElse(null);
        if (latest != null && "PENDING".equals(latest.status())) {
            throw new ApiException(HttpStatus.CONFLICT, "You already have a pending contributor application.");
        }

        StoredFile storedFile = storeSupportingFile(file, user.id());
        String portfolioLink = normalizeText(request.portfolioLink());
        String expertiseField = normalizeExpertiseField(request.expertiseField());
        if (portfolioLink == null && storedFile != null) {
            portfolioLink = storedFile.url();
        }

        Long id = contributorApplicationRepository.insert(
                user.id(),
                request.fullName().trim(),
                expertiseField,
                request.motivationStatement().trim(),
                portfolioLink,
                storedFile == null ? null : storedFile.originalName(),
                storedFile == null ? null : storedFile.url()
        );

        return contributorApplicationRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to save contributor application."));
    }

    /**
     * Returns all pending applications for administrator review.
     */
    public List<ContributorApplicationSummaryResponse> getPendingApplications() {
        return contributorApplicationRepository.findAllPending();
    }

    /**
     * Returns the details for a single contributor application.
     */
    public ContributorApplicationResponse getApplicationDetail(Long applicationId) {
        return contributorApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Contributor application not found."));
    }

    /**
     * Approves a contributor application and upgrades the user role.
     */
    @Transactional
    public ContributorApplicationResponse approve(Long applicationId, String operatorName) {
        ContributorApplicationResponse detail = getApplicationDetail(applicationId);
        requirePending(detail);
        contributorApplicationRepository.updateReview(applicationId, "APPROVED", null);
        userRepository.updateUser(
                detail.userId(),
                detail.email(),
                detail.username(),
                userRepository.findById(detail.userId())
                        .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found."))
                        .passwordHash(),
                UserRole.CONTRIBUTOR
        );
        adminActivityRepository.insert(
                "contributor approved",
                "Contributor",
                detail.fullName(),
                operator(operatorName),
                LocalDateTime.now(),
                "Contributor application approved. User role upgraded to contributor."
        );
        return getApplicationDetail(applicationId);
    }

    /**
     * Rejects a contributor application with review comments.
     */
    @Transactional
    public ContributorApplicationResponse reject(Long applicationId, String comments, String operatorName) {
        String normalizedComments = normalizeText(comments);
        if (normalizedComments == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Rejection comments are required.");
        }
        ContributorApplicationResponse detail = getApplicationDetail(applicationId);
        requirePending(detail);
        contributorApplicationRepository.updateReview(applicationId, "REJECTED", normalizedComments);
        adminActivityRepository.insert(
                "contributor rejected",
                "Contributor",
                detail.fullName(),
                operator(operatorName),
                LocalDateTime.now(),
                normalizedComments
        );
        return getApplicationDetail(applicationId);
    }

    /**
     * Ensures the application is still waiting for review.
     */
    private void requirePending(ContributorApplicationResponse application) {
        if (!"PENDING".equalsIgnoreCase(application.status())) {
            throw new ApiException(HttpStatus.CONFLICT, "Only pending contributor applications can be reviewed.");
        }
    }

    /**
     * Ensures the current user is eligible to submit an application.
     */
    private UserAccount requireEligibleUser(Long userId) {
        UserAccount user = requireSignedInUser(userId);
        if (user.role() != UserRole.USER) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only registered users can apply for contributor access.");
        }
        return user;
    }

    /**
     * Loads the signed-in user or throws an authentication error.
     */
    private UserAccount requireSignedInUser(Long userId) {
        if (userId == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Please log in to continue.");
        }
        return userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Please log in to continue."));
    }

    /**
     * Stores an optional supporting file for the application.
     */
    private StoredFile storeSupportingFile(MultipartFile file, Long userId) {
        if (file == null || file.isEmpty()) {
            return null;
        }

        if (file.getSize() > MAX_FILE_BYTES) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Supporting file must be 10MB or smaller.");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_FILE_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Only PDF, JPG, PNG, and WEBP files are supported.");
        }

        String originalName = file.getOriginalFilename() == null ? "supporting-file" : file.getOriginalFilename().trim();
        String extension = extensionOf(originalName, contentType);
        String storedName = "application-" + userId + "-" + LocalDateTime.now().getNano() + "-" + UUID.randomUUID() + extension;
        Path target = APPLICATION_UPLOAD_DIR.resolve(storedName).normalize();
        if (!target.startsWith(APPLICATION_UPLOAD_DIR)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid upload path.");
        }

        try {
            Files.createDirectories(APPLICATION_UPLOAD_DIR);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to store supporting file.");
        }

        return new StoredFile(originalName, "/uploads/applications/" + storedName);
    }

    /**
     * Resolves the file extension for the uploaded content.
     */
    private String extensionOf(String originalName, String contentType) {
        int dot = originalName.lastIndexOf('.');
        if (dot >= 0 && dot < originalName.length() - 1) {
            return originalName.substring(dot);
        }
        return switch (contentType.toLowerCase(Locale.ROOT)) {
            case "application/pdf" -> ".pdf";
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> "";
        };
    }

    /**
     * Trims a nullable text value and converts blanks to null.
     */
    private String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /**
     * Validates the selected expertise field against known categories.
     */
    private String normalizeExpertiseField(String value) {
        String normalized = normalizeText(value);
        List<String> categories = resourceRepository.findCategories();
        if (normalized == null || categories.stream().noneMatch(category -> normalized.equals(category == null ? null : category.trim()))) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Please choose one of the provided expertise fields.");
        }
        return normalized;
    }

    /**
     * Returns the operator name used for admin history entries.
     */
    private String operator(String operatorName) {
        String normalized = normalizeText(operatorName);
        return normalized == null ? "Admin Console" : normalized;
    }

    /**
     * Stored file projection used during application submission.
     */
    private record StoredFile(String originalName, String url) {
    }
}
