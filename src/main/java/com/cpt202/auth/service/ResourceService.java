package com.cpt202.auth.service;

import com.cpt202.auth.dto.PageResponse;
import com.cpt202.auth.dto.ResourceDetail;
import com.cpt202.auth.dto.ResourceSubmissionDto;
import com.cpt202.auth.dto.ResourceSummary;
import com.cpt202.auth.exception.ApiException;
import com.cpt202.auth.model.HeritageResource;
import com.cpt202.auth.repository.ResourceRepository;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
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
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final ResourceRepository resourceRepository;
    private final SubmissionEmailService submissionEmailService;

    public ResourceService(ResourceRepository resourceRepository,
                           SubmissionEmailService submissionEmailService) {
        this.resourceRepository = resourceRepository;
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

    public ResourceDetail getResource(Long resourceId) {
        HeritageResource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Resource not found."));
        return toDetail(resource);
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

    public HeritageResource submitResource(ResourceSubmissionDto dto) {
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
                : resourceRepository.findAnyById(dto.id())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Draft not found."));

        if (existing != null && !"DRAFT".equalsIgnoreCase(existing.status())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Only draft resources can be submitted for review.");
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
                existing == null ? LocalDateTime.now() : existing.createdAt()
        );
        HeritageResource saved = existing == null
                ? resourceRepository.insert(resource)
                : resourceRepository.updateDraft(resource);
        resourceRepository.replaceTags(saved.id(), normalizeTags(dto.tags()));
        return saved;
    }

    public HeritageResource createDraft(ResourceSubmissionDto dto) {
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
            HeritageResource existing = resourceRepository.findDraftById(dto.id())
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Draft not found."));

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
                    existing.createdAt()
            );
            HeritageResource saved = resourceRepository.updateDraft(updated);
            resourceRepository.replaceTags(saved.id(), normalizeTags(dto.tags()));
            return saved;
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
                LocalDateTime.now()
        );
        HeritageResource saved = resourceRepository.insert(resource);
        resourceRepository.replaceTags(saved.id(), normalizeTags(dto.tags()));
        return saved;
    }

    public HeritageResource getDraft(Long resourceId) {
        HeritageResource resource = resourceRepository.findAnyById(resourceId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Draft not found."));
        if (!"DRAFT".equalsIgnoreCase(resource.status()) && !"PENDING".equalsIgnoreCase(resource.status())) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Draft not found.");
        }
        return resource;
    }

    public List<String> getResourceTags(Long resourceId) {
        return resourceRepository.findTagsByResourceId(resourceId);
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

    private ResourceDetail toDetail(HeritageResource resource) {
        return new ResourceDetail(
                resource.id(),
                resource.title(),
                resource.titleEn(),
                resource.category(),
                resource.place(),
                resource.description(),
                resource.thumbnail(),
                resource.copyright(),
                resource.status(),
                resource.viewCount(),
                formatDate(resource.createdAt()),
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

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String trimToNull(String value) {
        return hasText(value) ? value.trim() : null;
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
