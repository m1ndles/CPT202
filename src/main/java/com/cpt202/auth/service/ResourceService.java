package com.cpt202.auth.service;

import com.cpt202.auth.dto.PageResponse;
import com.cpt202.auth.dto.MyResourceItemResponse;
import com.cpt202.auth.dto.ResourceAppealMessageResponse;
import com.cpt202.auth.dto.ResourceAppealSubmissionResponse;
import com.cpt202.auth.dto.ResourceDetail;
import com.cpt202.auth.dto.ResourceFavoriteResponse;
import com.cpt202.auth.dto.ResourceSubmissionDto;
import com.cpt202.auth.dto.ResourceSummary;
import com.cpt202.auth.exception.ApiException;
import com.cpt202.auth.model.HeritageResource;
import com.cpt202.auth.repository.AdminArchiveRepository;
import com.cpt202.auth.repository.ResourceAppealMessageRepository;
import com.cpt202.auth.repository.ResourceRepository;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/**
 * Resource business logic.
 */
@Service
public class ResourceService {

    private static final int DEFAULT_PAGE_SIZE = 6;
    private static final int MAX_PAGE_SIZE = 50;
    private static final int MAX_DRAFTS_PER_USER = 50;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final Set<String> MY_RESOURCE_STATUSES = Set.of("DRAFT", "PENDING", "APPROVED", "REJECTED");

    private final ResourceRepository resourceRepository;
    private final AdminArchiveRepository adminArchiveRepository;
    private final ResourceAppealMessageRepository resourceAppealMessageRepository;
    private final DraftAttachmentService draftAttachmentService;
    private final SubmissionEmailService submissionEmailService;

    public ResourceService(ResourceRepository resourceRepository,
                           AdminArchiveRepository adminArchiveRepository,
                           ResourceAppealMessageRepository resourceAppealMessageRepository,
                           DraftAttachmentService draftAttachmentService,
                           SubmissionEmailService submissionEmailService) {
        this.resourceRepository = resourceRepository;
        this.adminArchiveRepository = adminArchiveRepository;
        this.resourceAppealMessageRepository = resourceAppealMessageRepository;
        this.draftAttachmentService = draftAttachmentService;
        this.submissionEmailService = submissionEmailService;
    }

    public PageResponse<ResourceSummary> getResources(String keyword, String category, String place,
                                                      String sort, int page, int size) {
        int safePage = normalizePage(page);
        int safeSize = normalizeSize(size, DEFAULT_PAGE_SIZE);
        long totalItems = resourceRepository.countApproved(keyword, category, place);
        int totalPages = totalItems == 0 ? 0 : (int) Math.ceil((double) totalItems / safeSize);
        int offset = (safePage - 1) * safeSize;

        List<ResourceSummary> content = resourceRepository.findApproved(
                        keyword,
                        category,
                        place,
                        normalizeSort(sort),
                        offset,
                        safeSize
                ).stream()
                .map(this::toSummary)
                .toList();

        return new PageResponse<>(content, safePage, safeSize, totalItems, totalPages);
    }

    public ResourceDetail getResource(Long resourceId, Long currentUserId) {
        HeritageResource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Resource not found."));
        return toDetail(resource, "", List.of(), false, currentUserId);
    }

    public List<String> getCategories() {
        return resourceRepository.findCategories();
    }

    public List<String> getPlaces() {
        return resourceRepository.findPlaces();
    }

    public int incrementView(Long resourceId) {
        if (resourceRepository.findById(resourceId).isEmpty()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Resource not found.");
        }
        resourceRepository.incrementViewCount(resourceId);
        return resourceRepository.getViewCount(resourceId);
    }

    public ResourceFavoriteResponse toggleFavorite(Long resourceId, Long userId) {
        HeritageResource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Resource not found."));

        boolean favorited = resourceRepository.isFavoritedByUser(resource.id(), userId);
        if (favorited) {
            resourceRepository.removeFavorite(resource.id(), userId);
        } else {
            resourceRepository.addFavorite(resource.id(), userId);
        }

        boolean updatedState = !favorited;
        return new ResourceFavoriteResponse(
                updatedState ? "Resource saved to favorites." : "Resource removed from favorites.",
                updatedState,
                resourceRepository.countFavoritesByResourceId(resource.id())
        );
    }

    public HeritageResource submitResource(ResourceSubmissionDto dto, Long ownerUserId, String ownerUsername) {
        if (dto == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Request body is required.");
        }
        if (!hasText(dto.title())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Title is required.");
        }
        if (!hasText(dto.description())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Description is required.");
        }
        if (!hasText(dto.category())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Category is required.");
        }
        if (!hasText(dto.place())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Place is required.");
        }

        HeritageResource existing = dto.id() == null
                ? null
                : requireOwnedResource(dto.id(), ownerUserId);

        if (existing != null && !isResubmittableStatus(existing.status())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Only draft or rejected resources can be submitted for review.");
        }

        String trackingId = existing != null && hasText(existing.trackingId())
                ? existing.trackingId()
                : generateTrackingId();

        HeritageResource resource = new HeritageResource(
                existing == null ? null : existing.id(),
                dto.title().trim(),
                trimToNull(dto.titleEn()),
                dto.category().trim(),
                trimToNull(dto.period()),
                dto.place().trim(),
                trimToNull(dto.description()),
                trimToNull(dto.thumbnail()),
                trimToNull(dto.copyright()),
                trackingId,
                "PENDING",
                existing == null ? 0 : existing.viewCount(),
                existing == null ? LocalDateTime.now() : existing.createdAt(),
                existing == null ? ownerUserId : existing.ownerUserId(),
                existing == null ? normalizeOwnerUsername(ownerUsername) : normalizeOwnerUsername(existing.ownerUsername(), ownerUsername)
        );
        HeritageResource saved = existing == null
                ? resourceRepository.insert(resource)
                : resourceRepository.updateDraft(resource);
        resourceRepository.replaceTags(saved.id(), normalizeTags(dto.tags()));
        return saved;
    }

    public HeritageResource createDraft(ResourceSubmissionDto dto, Long ownerUserId, String ownerUsername) {
        if (dto == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Request body is required.");
        }
        if (!hasText(dto.title())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Title is required.");
        }
        if (!hasText(dto.description())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Description is required.");
        }

        if (dto.id() != null) {
            HeritageResource existing = requireOwnedResource(dto.id(), ownerUserId);
            if (!isEditableDraftStatus(existing.status())) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Only draft or rejected resources can be edited.");
            }

            HeritageResource updated = new HeritageResource(
                    existing.id(),
                    dto.title().trim(),
                    trimToNull(dto.titleEn()),
                    hasText(dto.category()) ? dto.category().trim() : existing.category(),
                    trimToNull(dto.period()),
                    hasText(dto.place()) ? dto.place().trim() : existing.place(),
                    dto.description().trim(),
                    trimToNull(dto.thumbnail()),
                    trimToNull(dto.copyright()),
                    existing.trackingId(),
                    "DRAFT",
                    existing.viewCount(),
                    existing.createdAt(),
                    existing.ownerUserId(),
                    normalizeOwnerUsername(existing.ownerUsername(), ownerUsername)
            );
            HeritageResource saved = resourceRepository.updateDraft(updated);
            resourceRepository.replaceTags(saved.id(), normalizeTags(dto.tags()));
            return saved;
        }

        long draftCount = resourceRepository.countByOwnerAndStatus(ownerUserId, "DRAFT");
        if (draftCount >= MAX_DRAFTS_PER_USER) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "You can keep up to 50 saved drafts. Please delete an older draft before creating a new one.");
        }

        HeritageResource resource = new HeritageResource(
                null,
                dto.title().trim(),
                trimToNull(dto.titleEn()),
                hasText(dto.category()) ? dto.category().trim() : "Uncategorized",
                trimToNull(dto.period()),
                hasText(dto.place()) ? dto.place().trim() : "Unknown",
                dto.description().trim(),
                trimToNull(dto.thumbnail()),
                trimToNull(dto.copyright()),
                null,
                "DRAFT",
                0,
                LocalDateTime.now(),
                ownerUserId,
                normalizeOwnerUsername(ownerUsername)
        );
        HeritageResource saved = resourceRepository.insert(resource);
        resourceRepository.replaceTags(saved.id(), normalizeTags(dto.tags()));
        return saved;
    }

    public HeritageResource getOwnedDraft(Long resourceId, Long ownerUserId) {
        HeritageResource resource = requireOwnedResource(resourceId, ownerUserId);
        if (!isDraftLikeStatus(resource.status())) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Draft not found.");
        }
        return resource;
    }

    public HeritageResource getOwnedEditableDraft(Long resourceId, Long ownerUserId) {
        HeritageResource resource = requireOwnedResource(resourceId, ownerUserId);
        if (!isEditableDraftStatus(resource.status())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Only draft or rejected resources can be edited.");
        }
        return resource;
    }

    public void deleteOwnedResource(Long resourceId, Long ownerUserId) {
        HeritageResource resource = requireOwnedResource(resourceId, ownerUserId);
        if (!isDeletableStatus(resource.status())) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Only draft and published resources can be deleted from My Resources.");
        }

        draftAttachmentService.removeStoredFilesForResource(resource.id());
        resourceRepository.deleteResource(resource.id(), ownerUserId);
    }

    public List<MyResourceItemResponse> getMyResources(Long ownerUserId, String status) {
        String normalizedStatus = normalizeMyResourceStatus(status);
        return resourceRepository.findByOwner(ownerUserId, normalizedStatus).stream()
                .map(resource -> new MyResourceItemResponse(
                        resource.id(),
                        resource.title(),
                        resource.category(),
                        resource.place(),
                        resource.description(),
                        resource.thumbnail(),
                        resource.trackingId(),
                        resource.status(),
                        formatDate(resource.createdAt()),
                        resourceRepository.findTagsByResourceId(resource.id())
                ))
                .toList();
    }

    public List<ResourceSummary> getMyFavoriteResources(Long userId) {
        return resourceRepository.findFavoritesByUser(userId).stream()
                .map(this::toSummary)
                .toList();
    }

    public List<String> getResourceTags(Long resourceId) {
        return resourceRepository.findTagsByResourceId(resourceId);
    }

    public String getRevisionFeedback(Long resourceId, Long ownerUserId) {
        HeritageResource resource = requireOwnedResource(resourceId, ownerUserId);
        return loadRevisionFeedback(resource.id());
    }

    public List<ResourceAppealMessageResponse> getAppealMessages(Long resourceId, Long ownerUserId) {
        HeritageResource resource = requireOwnedResource(resourceId, ownerUserId);
        return resourceAppealMessageRepository.findByResourceId(resource.id());
    }

    public boolean canSendAppeal(Long resourceId, Long ownerUserId) {
        HeritageResource resource = requireOwnedResource(resourceId, ownerUserId);
        return canSendAppeal(resource);
    }

    public ResourceAppealSubmissionResponse submitAppeal(Long resourceId,
                                                         Long ownerUserId,
                                                         String senderName,
                                                         String content) {
        HeritageResource resource = requireOwnedResource(resourceId, ownerUserId);
        if (!canSendAppeal(resource)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "This resource is not currently available for appeal messages.");
        }

        String normalizedContent = trimToNull(content);
        if (!hasText(normalizedContent)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Appeal message content is required.");
        }
        if (normalizedContent.length() > 1000) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Appeal message must be 1000 characters or fewer.");
        }

        LocalDateTime now = LocalDateTime.now();
        resourceAppealMessageRepository.insert(
                resource.id(),
                "CONTRIBUTOR",
                normalizeOwnerUsername(senderName),
                normalizedContent,
                now
        );

        return new ResourceAppealSubmissionResponse(
                "Message sent to the admin review team.",
                resourceAppealMessageRepository.findByResourceId(resource.id())
        );
    }

    public void sendSubmissionConfirmation(String email, HeritageResource resource) {
        if (!hasText(email) || resource == null) {
            return;
        }
        submissionEmailService.sendSubmissionConfirmation(email.trim(), resource);
    }

    private ResourceSummary toSummary(HeritageResource resource) {
        List<String> tags = resourceRepository.findTagsByResourceId(resource.id());
        if (tags.size() > 3) {
            tags = tags.subList(0, 3);
        }
        return new ResourceSummary(
                resource.id(),
                resource.title(),
                resource.category(),
                resource.place(),
                resource.thumbnail(),
                tags,
                resource.viewCount(),
                formatDate(resource.createdAt())
        );
    }

    private ResourceDetail toDetail(HeritageResource resource,
                                    String rejectionComments,
                                    List<ResourceAppealMessageResponse> appealMessages,
                                    boolean canSendAppeal,
                                    Long currentUserId) {
        return new ResourceDetail(
                resource.id(),
                resource.title(),
                resource.titleEn(),
                resource.category(),
                resource.period(),
                resource.place(),
                resource.description(),
                resource.thumbnail(),
                resource.copyright(),
                resource.trackingId(),
                resource.status(),
                resource.viewCount(),
                resourceRepository.countFavoritesByResourceId(resource.id()),
                resourceRepository.isFavoritedByUser(resource.id(), currentUserId),
                formatDate(resource.createdAt()),
                rejectionComments,
                appealMessages,
                canSendAppeal,
                resourceRepository.findTagsByResourceId(resource.id()),
                resourceRepository.findFilesByResourceId(resource.id()),
                resourceRepository.findLinksByResourceId(resource.id())
        );
    }

    private String normalizeSort(String sort) {
        if ("views".equals(sort) || "title".equals(sort)) {
            return sort;
        }
        return "newest";
    }

    private int normalizePage(int page) {
        return Math.max(page, 1);
    }

    private int normalizeSize(int size, int defaultSize) {
        if (size <= 0) {
            return defaultSize;
        }
        return Math.min(size, MAX_PAGE_SIZE);
    }

    private String formatDate(java.time.LocalDateTime value) {
        return value == null ? "" : DATE_FORMATTER.format(value);
    }

    private HeritageResource requireOwnedResource(Long resourceId, Long ownerUserId) {
        if (ownerUserId == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Please log in to continue.");
        }
        return resourceRepository.findAnyByIdAndOwner(resourceId, ownerUserId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Resource not found."));
    }

    private String normalizeMyResourceStatus(String status) {
        if (!hasText(status)) {
            return "";
        }
        String normalized = status.trim().toUpperCase(Locale.ROOT);
        if (!MY_RESOURCE_STATUSES.contains(normalized)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Unsupported resource status filter.");
        }
        return normalized;
    }

    private boolean isDraftLikeStatus(String status) {
        String normalized = String.valueOf(status).toUpperCase(Locale.ROOT);
        return "DRAFT".equals(normalized) || "PENDING".equals(normalized) || "REJECTED".equals(normalized);
    }

    private boolean isEditableDraftStatus(String status) {
        String normalized = String.valueOf(status).toUpperCase(Locale.ROOT);
        return "DRAFT".equals(normalized) || "REJECTED".equals(normalized);
    }

    private boolean isResubmittableStatus(String status) {
        return isEditableDraftStatus(status);
    }

    private boolean isDeletableStatus(String status) {
        String normalized = String.valueOf(status).toUpperCase(Locale.ROOT);
        return "DRAFT".equals(normalized) || "APPROVED".equals(normalized);
    }

    private boolean canSendAppeal(HeritageResource resource) {
        return resource != null
                && isEditableDraftStatus(resource.status())
                && hasText(loadRevisionFeedback(resource.id()));
    }

    private String loadRevisionFeedback(Long resourceId) {
        return adminArchiveRepository.findByResourceId(resourceId)
                .map(AdminArchiveRepository.ArchiveRecord::archiveReason)
                .filter(this::hasText)
                .map(String::trim)
                .orElse("");
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String trimToNull(String value) {
        return hasText(value) ? value.trim() : null;
    }

    private String normalizeOwnerUsername(String primary, String fallback) {
        if (hasText(primary)) {
            return primary.trim();
        }
        return normalizeOwnerUsername(fallback);
    }

    private String normalizeOwnerUsername(String value) {
        return hasText(value) ? value.trim() : "Contributor";
    }

    private List<String> normalizeTags(String value) {
        if (!hasText(value)) {
            return List.of();
        }
        return java.util.Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(this::hasText)
                .distinct()
                .toList();
    }

    private String generateTrackingId() {
        return "TRK-" + UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 10)
                .toUpperCase(Locale.ROOT);
    }
}
