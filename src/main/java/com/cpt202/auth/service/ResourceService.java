package com.cpt202.auth.service;

import com.cpt202.auth.dto.PageResponse;
import com.cpt202.auth.dto.ResourceDetail;
import com.cpt202.auth.dto.ResourceSummary;
import com.cpt202.auth.exception.ApiException;
import com.cpt202.auth.model.HeritageResource;
import com.cpt202.auth.repository.ResourceRepository;
import java.time.format.DateTimeFormatter;
import java.util.List;
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

    public ResourceService(ResourceRepository resourceRepository) {
        this.resourceRepository = resourceRepository;
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
}
