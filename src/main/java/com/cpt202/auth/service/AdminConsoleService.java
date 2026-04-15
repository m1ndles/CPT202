package com.cpt202.auth.service;

import com.cpt202.auth.dto.ContributorApplicationResponse;
import com.cpt202.auth.dto.ContributorApplicationSummaryResponse;
import com.cpt202.auth.dto.ResourceDetail;
import com.cpt202.auth.dto.admin.AdminActionResponse;
import com.cpt202.auth.dto.admin.AdminArchiveItemResponse;
import com.cpt202.auth.dto.admin.AdminCategoryItemResponse;
import com.cpt202.auth.dto.admin.AdminDashboardSummaryResponse;
import com.cpt202.auth.dto.admin.AdminDashboardSummaryResponse.AdminDashboardBreakdownItem;
import com.cpt202.auth.dto.admin.AdminDashboardSummaryResponse.AdminDashboardInsightSection;
import com.cpt202.auth.dto.admin.AdminDashboardSummaryResponse.AdminDashboardMetricCard;
import com.cpt202.auth.dto.admin.AdminDashboardSummaryResponse.AdminDashboardTrendPoint;
import com.cpt202.auth.dto.admin.AdminHistoryItemResponse;
import com.cpt202.auth.dto.admin.AdminResourceReviewDetailResponse;
import com.cpt202.auth.dto.admin.AdminResourceReviewItemResponse;
import com.cpt202.auth.dto.admin.AdminTagItemResponse;
import com.cpt202.auth.dto.admin.AdminTaxonomyRequest;
import com.cpt202.auth.exception.ApiException;
import com.cpt202.auth.model.HeritageResource;
import com.cpt202.auth.repository.AdminActivityRepository;
import com.cpt202.auth.repository.AdminArchiveRepository;
import com.cpt202.auth.repository.AdminTaxonomyRepository;
import com.cpt202.auth.repository.ContributorApplicationRepository;
import com.cpt202.auth.repository.ResourceRepository;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminConsoleService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    private static final String SYSTEM_OPERATOR = "Admin Console";

    private final ContributorApplicationRepository contributorApplicationRepository;
    private final ContributorApplicationService contributorApplicationService;
    private final ResourceRepository resourceRepository;
    private final AdminTaxonomyRepository adminTaxonomyRepository;
    private final AdminArchiveRepository adminArchiveRepository;
    private final AdminActivityRepository adminActivityRepository;

    public AdminConsoleService(ContributorApplicationRepository contributorApplicationRepository,
                               ContributorApplicationService contributorApplicationService,
                               ResourceRepository resourceRepository,
                               AdminTaxonomyRepository adminTaxonomyRepository,
                               AdminArchiveRepository adminArchiveRepository,
                               AdminActivityRepository adminActivityRepository) {
        this.contributorApplicationRepository = contributorApplicationRepository;
        this.contributorApplicationService = contributorApplicationService;
        this.resourceRepository = resourceRepository;
        this.adminTaxonomyRepository = adminTaxonomyRepository;
        this.adminArchiveRepository = adminArchiveRepository;
        this.adminActivityRepository = adminActivityRepository;
    }

    @Transactional
    public void initializeTaxonomyIfEmpty() {
        if (adminTaxonomyRepository.countCategories() == 0) {
            resourceRepository.findAllResources().stream()
                    .map(HeritageResource::category)
                    .filter(this::hasText)
                    .map(String::trim)
                    .distinct()
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .forEach(name -> adminTaxonomyRepository.insertCategory(
                            name,
                            "Imported from existing heritage resource records.",
                            "ACTIVE",
                            LocalDateTime.now()
                    ));
        }

        if (adminTaxonomyRepository.countTags() == 0) {
            resourceRepository.findAllResources().stream()
                    .flatMap(resource -> resourceRepository.findTagsByResourceId(resource.id()).stream())
                    .filter(this::hasText)
                    .map(String::trim)
                    .distinct()
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .forEach(name -> adminTaxonomyRepository.insertTag(
                            name,
                            "Imported from existing heritage resource records.",
                            "ACTIVE",
                            LocalDateTime.now()
                    ));
        }
    }

    public AdminDashboardSummaryResponse getDashboardSummary() {
        initializeTaxonomyIfEmpty();

        List<ContributorApplicationSummaryResponse> allApplications = contributorApplicationRepository.findAllApplications();
        List<HeritageResource> allResources = resourceRepository.findAllResources();
        List<AdminCategoryItemResponse> categories = getCategories();
        List<AdminTagItemResponse> tags = getTags();
        List<AdminArchiveItemResponse> archives = getArchiveItems();
        List<AdminHistoryItemResponse> recentHistory = getHistoryItems().stream().limit(5).toList();

        ContributorApplicationSummaryResponse latestApplicant = allApplications.stream()
                .max(Comparator.comparing(item -> parseDateTime(item.submittedAt())))
                .orElse(null);
        HeritageResource latestPendingResource = allResources.stream()
                .filter(resource -> "PENDING".equalsIgnoreCase(resource.status()))
                .max(Comparator.comparing(HeritageResource::createdAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .orElse(null);
        AdminCategoryItemResponse latestCategory = categories.stream()
                .max(Comparator.comparing(item -> parseDate(item.updatedAt())))
                .orElse(null);
        AdminTagItemResponse latestTag = tags.stream()
                .max(Comparator.comparing(item -> parseDate(item.updatedAt())))
                .orElse(null);
        AdminArchiveItemResponse latestArchive = archives.stream()
                .max(Comparator.comparing(item -> parseDate(item.archivedAt())))
                .orElse(null);

        int pendingContributorApplications = (int) allApplications.stream().filter(item -> "PENDING".equalsIgnoreCase(item.status())).count();
        int pendingResourceReviews = (int) allResources.stream().filter(item -> "PENDING".equalsIgnoreCase(item.status())).count();
        int approvedContributors = (int) allApplications.stream().filter(item -> "APPROVED".equalsIgnoreCase(item.status())).count();
        int approvedResources = (int) allResources.stream().filter(item -> "APPROVED".equalsIgnoreCase(item.status())).count();
        int rejectedApplications = (int) allApplications.stream().filter(item -> "REJECTED".equalsIgnoreCase(item.status())).count();
        int rejectedResources = (int) allResources.stream().filter(item -> "REJECTED".equalsIgnoreCase(item.status())).count();
        int archivedResources = archives.size();
        int activeCategories = (int) categories.stream().filter(item -> "ACTIVE".equalsIgnoreCase(item.status())).count();
        int activeTags = (int) tags.stream().filter(item -> "ACTIVE".equalsIgnoreCase(item.status())).count();

        List<AdminDashboardMetricCard> metricCards = List.of(
                new AdminDashboardMetricCard("Pending Contributor Applications", String.valueOf(pendingContributorApplications), "priority"),
                new AdminDashboardMetricCard("Pending Resource Reviews", String.valueOf(pendingResourceReviews), "priority"),
                new AdminDashboardMetricCard("Approved Contributors", String.valueOf(approvedContributors), "success"),
                new AdminDashboardMetricCard("Approved Resources", String.valueOf(approvedResources), "success"),
                new AdminDashboardMetricCard("Rejected Applications", String.valueOf(rejectedApplications), "warning"),
                new AdminDashboardMetricCard("Rejected Resources", String.valueOf(rejectedResources), "warning"),
                new AdminDashboardMetricCard("Archived Resources", String.valueOf(archivedResources), "neutral"),
                new AdminDashboardMetricCard("Active Categories / Tags", activeCategories + " / " + activeTags, "neutral")
        );

        return new AdminDashboardSummaryResponse(
                pendingContributorApplications + pendingResourceReviews + archivedResources,
                metricCards,
                buildContributionSection(allApplications, pendingContributorApplications, approvedContributors, rejectedApplications),
                buildResourceSection(allResources, pendingResourceReviews, approvedResources, rejectedResources, archivedResources),
                pendingContributorApplications,
                pendingResourceReviews,
                activeCategories,
                activeTags,
                latestApplicant == null ? "No applications yet" : latestApplicant.fullName() + " · " + latestApplicant.expertiseField(),
                latestPendingResource == null ? "No resources awaiting review" : latestPendingResource.title() + " · " + latestPendingResource.category(),
                latestCategory != null && latestTag != null
                        ? latestCategory.name() + " / " + latestTag.name()
                        : "No taxonomy updates yet",
                archivedResources,
                latestArchive == null ? "No archived resources" : latestArchive.title(),
                recentHistory
        );
    }

    private AdminDashboardInsightSection buildContributionSection(List<ContributorApplicationSummaryResponse> applications,
                                                                  int pendingCount,
                                                                  int approvedCount,
                                                                  int rejectedCount) {
        List<AdminDashboardTrendPoint> trend = buildWeeklyTrend(
                applications.stream().map(item -> parseDateTime(item.submittedAt())).toList()
        );
        List<AdminDashboardBreakdownItem> breakdown = buildTopBreakdown(
                applications.stream()
                        .filter(item -> hasText(item.expertiseField()))
                        .collect(Collectors.groupingBy(item -> item.expertiseField().trim(), Collectors.counting())),
                List.of("#0f766e", "#38bdf8", "#f59e0b", "#f97316")
        );

        List<String> todoItems = new ArrayList<>();
        if (pendingCount > 0) {
            todoItems.add(pendingCount + " contributor applications are still waiting for review.");
        }
        if (rejectedCount > 0) {
            todoItems.add(rejectedCount + " applications were rejected and may need clearer feedback or follow-up.");
        }
        if (applications.isEmpty()) {
            todoItems.add("No contributor applications have been submitted yet.");
        }
        if (todoItems.isEmpty()) {
            todoItems.add("Contributor approval flow is fully caught up right now.");
        }

        return new AdminDashboardInsightSection(
                "Contribution",
                "Application growth, field distribution, and review backlog",
                String.valueOf(applications.size()),
                "Total contributor applications",
                "Approved / Pending",
                approvedCount + " / " + pendingCount,
                trend,
                breakdown,
                todoItems
        );
    }

    private AdminDashboardInsightSection buildResourceSection(List<HeritageResource> resources,
                                                              int pendingCount,
                                                              int approvedCount,
                                                              int rejectedCount,
                                                              int archivedCount) {
        List<AdminDashboardTrendPoint> trend = buildWeeklyTrend(
                resources.stream()
                        .map(HeritageResource::createdAt)
                        .filter(Objects::nonNull)
                        .toList()
        );
        List<AdminDashboardBreakdownItem> breakdown = buildTopBreakdown(
                resources.stream()
                        .filter(resource -> hasText(resource.category()))
                        .collect(Collectors.groupingBy(resource -> resource.category().trim(), Collectors.counting())),
                List.of("#2563eb", "#06b6d4", "#14b8a6", "#f97316")
        );

        List<String> todoItems = new ArrayList<>();
        if (pendingCount > 0) {
            todoItems.add(pendingCount + " resources are waiting for moderation review.");
        }
        if (archivedCount > 0) {
            todoItems.add(archivedCount + " archived resources may need restoration checks.");
        }
        if (rejectedCount > 0) {
            todoItems.add(rejectedCount + " resources were rejected and may need contributor revisions.");
        }
        if (resources.isEmpty()) {
            todoItems.add("No resource submissions exist yet.");
        }
        if (todoItems.isEmpty()) {
            todoItems.add("Resource review queue is clear at the moment.");
        }

        return new AdminDashboardInsightSection(
                "Resource",
                "Submission growth, category mix, and moderation workload",
                String.valueOf(resources.size()),
                "Total resource submissions",
                "Approved / Pending",
                approvedCount + " / " + pendingCount,
                trend,
                breakdown,
                todoItems
        );
    }

    private List<AdminDashboardTrendPoint> buildWeeklyTrend(List<LocalDateTime> timestamps) {
        LocalDateTime today = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
        return java.util.stream.IntStream.rangeClosed(0, 5)
                .mapToObj(index -> {
                    LocalDateTime start = today.minusDays((5L - index) * 7L);
                    LocalDateTime end = start.plusDays(7);
                    int count = (int) timestamps.stream()
                            .filter(Objects::nonNull)
                            .filter(time -> !time.isBefore(start) && time.isBefore(end))
                            .count();
                    return new AdminDashboardTrendPoint(start.format(DateTimeFormatter.ofPattern("MM-dd")), count);
                })
                .toList();
    }

    private List<AdminDashboardBreakdownItem> buildTopBreakdown(Map<String, Long> countsByLabel, List<String> colors) {
        List<Map.Entry<String, Long>> topItems = countsByLabel.entrySet().stream()
                .sorted((left, right) -> Long.compare(right.getValue(), left.getValue()))
                .limit(4)
                .toList();
        if (topItems.isEmpty()) {
            return List.of(new AdminDashboardBreakdownItem("No data", 0, "#cbd5e1"));
        }
        return java.util.stream.IntStream.range(0, topItems.size())
                .mapToObj(index -> new AdminDashboardBreakdownItem(
                        topItems.get(index).getKey(),
                        topItems.get(index).getValue().intValue(),
                        colors.get(index % colors.size())
                ))
                .toList();
    }

    public List<AdminResourceReviewItemResponse> getResourceReviewList() {
        return resourceRepository.findAllResources().stream()
                .filter(resource -> "PENDING".equalsIgnoreCase(resource.status()))
                .sorted(Comparator.comparing(HeritageResource::createdAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(resource -> new AdminResourceReviewItemResponse(
                        resource.id(),
                        resource.title(),
                        contributorLabel(resource),
                        resource.category(),
                        resource.place(),
                        formatDate(resource.createdAt()),
                        "PENDING_REVIEW",
                        defaultImage(resource.thumbnail())
                ))
                .toList();
    }

    public AdminResourceReviewDetailResponse getResourceReviewDetail(Long resourceId) {
        HeritageResource resource = resourceRepository.findAnyById(resourceId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Resource not found."));

        List<ResourceRepository.AttachmentRecord> attachments = resourceRepository.findDraftAttachments(resourceId);
        List<ResourceDetail.LinkItem> links = resourceRepository.findLinksByResourceId(resourceId);
        String fileLink = attachments.isEmpty() ? null : attachments.get(0).url();
        String externalLink = links.isEmpty() ? null : links.get(0).url();
        String status = mapReviewStatus(resource.status());
        String archiveReason = adminArchiveRepository.findByResourceId(resource.id())
                .map(AdminArchiveRepository.ArchiveRecord::archiveReason)
                .orElse(null);

        return new AdminResourceReviewDetailResponse(
                resource.id(),
                resource.title(),
                resource.titleEn(),
                resource.description(),
                contributorLabel(resource),
                resource.category(),
                resource.place(),
                formatDate(resource.createdAt()),
                status,
                defaultImage(resource.thumbnail()),
                defaultImage(resource.thumbnail()),
                fileLink,
                externalLink,
                resourceRepository.findTagsByResourceId(resourceId),
                resource.copyright(),
                buildSubmissionMetadata(resource),
                "REJECTED".equalsIgnoreCase(resource.status()) ? archiveReason : null,
                "APPROVED".equalsIgnoreCase(resource.status())
        );
    }

    @Transactional
    public AdminActionResponse approveResourceReview(Long resourceId, String operatorName) {
        HeritageResource resource = requirePendingResource(resourceId);
        updateResourceStatus(resource, "APPROVED");
        adminArchiveRepository.deleteByResourceId(resource.id());
        recordHistory("resource approved", "Resource", resource.title(), operatorName,
                "Moved into public visible status.");
        return new AdminActionResponse("Resource approved and moved into public visible status.");
    }

    @Transactional
    public AdminActionResponse rejectResourceReview(Long resourceId, String rejectionComments, String operatorName) {
        HeritageResource resource = requirePendingResource(resourceId);
        String normalizedComments = requireText(rejectionComments, "Rejection comments are required.");
        updateResourceStatus(resource, "REJECTED");
        adminArchiveRepository.upsert(
                resource.id(),
                contributorLabel(resource),
                operator(operatorName),
                normalizedComments,
                "Rejected during moderation review and removed from public visibility.",
                buildSubmissionMetadata(resource),
                LocalDateTime.now()
        );
        recordHistory("resource rejected", "Resource", resource.title(), operatorName, normalizedComments);
        return new AdminActionResponse("Resource rejected and returned for revision.");
    }

    public List<AdminCategoryItemResponse> getCategories() {
        initializeTaxonomyIfEmpty();
        Map<String, Long> resourceCounts = resourceRepository.findAllResources().stream()
                .filter(resource -> hasText(resource.category()))
                .collect(Collectors.groupingBy(resource -> resource.category().trim(), Collectors.counting()));

        return adminTaxonomyRepository.findAllCategories().stream()
                .map(record -> new AdminCategoryItemResponse(
                        record.id(),
                        record.name(),
                        record.description(),
                        record.status(),
                        formatDate(record.updatedAt()),
                        resourceCounts.getOrDefault(record.name(), 0L).intValue()
                ))
                .toList();
    }

    @Transactional
    public AdminCategoryItemResponse createCategory(AdminTaxonomyRequest request, String operatorName) {
        initializeTaxonomyIfEmpty();
        String name = requireText(request.name(), "Category name is required.");
        String description = requireText(request.description(), "Category description is required.");
        adminTaxonomyRepository.findCategoryByName(name).ifPresent(existing -> {
            throw new ApiException(HttpStatus.CONFLICT, "Category already exists.");
        });

        LocalDateTime now = LocalDateTime.now();
        Long id = adminTaxonomyRepository.insertCategory(name, description, "ACTIVE", now);
        recordHistory("category created", "Category", name, operatorName, description);
        return getCategories().stream()
                .filter(item -> Objects.equals(item.id(), id))
                .findFirst()
                .orElseThrow(() -> new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to create category."));
    }

    @Transactional
    public AdminCategoryItemResponse updateCategory(Long categoryId, AdminTaxonomyRequest request, String operatorName) {
        initializeTaxonomyIfEmpty();
        AdminTaxonomyRepository.TaxonomyRecord existing = adminTaxonomyRepository.findCategoryById(categoryId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Category not found."));
        String name = requireText(request.name(), "Category name is required.");
        String description = requireText(request.description(), "Category description is required.");
        adminTaxonomyRepository.findCategoryByName(name)
                .filter(record -> !Objects.equals(record.id(), categoryId))
                .ifPresent(record -> {
                    throw new ApiException(HttpStatus.CONFLICT, "Category name already exists.");
                });

        adminTaxonomyRepository.updateCategory(categoryId, name, description, existing.status(), LocalDateTime.now());
        recordHistory("category updated", "Category", name, operatorName, "Category details updated.");
        return getCategories().stream()
                .filter(item -> Objects.equals(item.id(), categoryId))
                .findFirst()
                .orElseThrow(() -> new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to update category."));
    }

    @Transactional
    public AdminCategoryItemResponse toggleCategoryStatus(Long categoryId, String operatorName) {
        initializeTaxonomyIfEmpty();
        AdminTaxonomyRepository.TaxonomyRecord existing = adminTaxonomyRepository.findCategoryById(categoryId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Category not found."));
        String nextStatus = "ACTIVE".equalsIgnoreCase(existing.status()) ? "INACTIVE" : "ACTIVE";
        adminTaxonomyRepository.updateCategoryStatus(categoryId, nextStatus, LocalDateTime.now());
        recordHistory("category status changed", "Category", existing.name(), operatorName,
                "Category marked " + nextStatus.toLowerCase(Locale.ROOT) + ".");
        return getCategories().stream()
                .filter(item -> Objects.equals(item.id(), categoryId))
                .findFirst()
                .orElseThrow(() -> new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to update category status."));
    }

    public List<AdminTagItemResponse> getTags() {
        initializeTaxonomyIfEmpty();
        Map<String, Long> usageCounts = resourceRepository.findAllResources().stream()
                .flatMap(resource -> resourceRepository.findTagsByResourceId(resource.id()).stream())
                .filter(this::hasText)
                .collect(Collectors.groupingBy(String::trim, Collectors.counting()));

        return adminTaxonomyRepository.findAllTags().stream()
                .map(record -> new AdminTagItemResponse(
                        record.id(),
                        record.name(),
                        record.description(),
                        record.status(),
                        formatDate(record.updatedAt()),
                        usageCounts.getOrDefault(record.name(), 0L).intValue()
                ))
                .toList();
    }

    @Transactional
    public AdminTagItemResponse createTag(AdminTaxonomyRequest request, String operatorName) {
        initializeTaxonomyIfEmpty();
        String name = requireText(request.name(), "Tag name is required.");
        String description = requireText(request.description(), "Tag description is required.");
        adminTaxonomyRepository.findTagByName(name).ifPresent(existing -> {
            throw new ApiException(HttpStatus.CONFLICT, "Tag already exists.");
        });

        LocalDateTime now = LocalDateTime.now();
        Long id = adminTaxonomyRepository.insertTag(name, description, "ACTIVE", now);
        recordHistory("tag created", "Tag", name, operatorName, description);
        return getTags().stream()
                .filter(item -> Objects.equals(item.id(), id))
                .findFirst()
                .orElseThrow(() -> new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to create tag."));
    }

    @Transactional
    public AdminTagItemResponse updateTag(Long tagId, AdminTaxonomyRequest request, String operatorName) {
        initializeTaxonomyIfEmpty();
        AdminTaxonomyRepository.TaxonomyRecord existing = adminTaxonomyRepository.findTagById(tagId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Tag not found."));
        String name = requireText(request.name(), "Tag name is required.");
        String description = requireText(request.description(), "Tag description is required.");
        adminTaxonomyRepository.findTagByName(name)
                .filter(record -> !Objects.equals(record.id(), tagId))
                .ifPresent(record -> {
                    throw new ApiException(HttpStatus.CONFLICT, "Tag name already exists.");
                });

        adminTaxonomyRepository.updateTag(tagId, name, description, existing.status(), LocalDateTime.now());
        recordHistory("tag updated", "Tag", name, operatorName, "Tag details updated.");
        return getTags().stream()
                .filter(item -> Objects.equals(item.id(), tagId))
                .findFirst()
                .orElseThrow(() -> new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to update tag."));
    }

    @Transactional
    public AdminTagItemResponse toggleTagStatus(Long tagId, String operatorName) {
        initializeTaxonomyIfEmpty();
        AdminTaxonomyRepository.TaxonomyRecord existing = adminTaxonomyRepository.findTagById(tagId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Tag not found."));
        String nextStatus = "ACTIVE".equalsIgnoreCase(existing.status()) ? "INACTIVE" : "ACTIVE";
        adminTaxonomyRepository.updateTagStatus(tagId, nextStatus, LocalDateTime.now());
        recordHistory("tag status changed", "Tag", existing.name(), operatorName,
                "Tag marked " + nextStatus.toLowerCase(Locale.ROOT) + ".");
        return getTags().stream()
                .filter(item -> Objects.equals(item.id(), tagId))
                .findFirst()
                .orElseThrow(() -> new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to update tag status."));
    }

    public List<AdminArchiveItemResponse> getArchiveItems() {
        return adminArchiveRepository.findAll().stream()
                .map(record -> new AdminArchiveItemResponse(
                        record.id(),
                        record.resourceId(),
                        record.title(),
                        valueOrFallback(record.contributorLabel(), "Contributor"),
                        record.category(),
                        formatDate(record.archivedAt()),
                        record.archivedBy(),
                        valueOrFallback(record.archiveReason(), "No archive reason was recorded."),
                        "ARCHIVED",
                        valueOrFallback(record.publicationHistory(), "No publication history available."),
                        valueOrFallback(record.originalMetadata(), "No original metadata available.")
                ))
                .toList();
    }

    public AdminArchiveItemResponse getArchiveDetail(Long archiveId) {
        return getArchiveItems().stream()
                .filter(item -> Objects.equals(item.id(), archiveId))
                .findFirst()
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Archive record not found."));
    }

    @Transactional
    public AdminActionResponse restoreArchive(Long archiveId, String operatorName) {
        AdminArchiveRepository.ArchiveRecord archiveRecord = adminArchiveRepository.findById(archiveId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Archive record not found."));
        HeritageResource resource = resourceRepository.findAnyById(archiveRecord.resourceId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Resource not found."));
        updateResourceStatus(resource, "APPROVED");
        adminArchiveRepository.deleteById(archiveId);
        recordHistory("resource restored", "Archive", archiveRecord.title(), operatorName,
                "Archived resource restored to active lifecycle.");
        return new AdminActionResponse("Resource restored from archive.");
    }

    public List<AdminHistoryItemResponse> getHistoryItems() {
        seedHistoryIfEmpty();
        return adminActivityRepository.findAll().stream()
                .map(record -> new AdminHistoryItemResponse(
                        record.actionType(),
                        record.targetType(),
                        record.targetName(),
                        record.operatorName(),
                        formatDateTime(record.createdAt()),
                        record.details()
                ))
                .toList();
    }

    private void seedHistoryIfEmpty() {
        if (adminActivityRepository.count() > 0) {
            return;
        }

        contributorApplicationRepository.findAllApplications().forEach(item -> {
            String details = switch (item.status().toUpperCase(Locale.ROOT)) {
                case "APPROVED" -> "Contributor role granted.";
                case "REJECTED" -> "Contributor application rejected.";
                default -> "Contributor application submitted.";
            };
            adminActivityRepository.insert(
                    "contributor " + item.status().toLowerCase(Locale.ROOT),
                    "Contributor",
                    item.fullName(),
                    SYSTEM_OPERATOR,
                    parseDateTime(item.submittedAt()),
                    details
            );
        });

        resourceRepository.findAllResources().forEach(resource -> {
            adminActivityRepository.insert(
                    "resource " + resource.status().toLowerCase(Locale.ROOT),
                    "Resource",
                    resource.title(),
                    SYSTEM_OPERATOR,
                    resource.createdAt() == null ? LocalDateTime.now() : resource.createdAt(),
                    "Moderation state is " + resource.status().toLowerCase(Locale.ROOT) + "."
            );
        });
    }

    private HeritageResource requirePendingResource(Long resourceId) {
        HeritageResource resource = resourceRepository.findAnyById(resourceId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Resource not found."));
        if (!"PENDING".equalsIgnoreCase(resource.status())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Only pending resources can be reviewed.");
        }
        return resource;
    }

    private void updateResourceStatus(HeritageResource resource, String nextStatus) {
        resourceRepository.updateDraft(new HeritageResource(
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
                nextStatus,
                resource.viewCount(),
                resource.createdAt()
        ));
    }

    private void recordHistory(String actionType,
                               String targetType,
                               String targetName,
                               String operatorName,
                               String details) {
        adminActivityRepository.insert(
                actionType,
                targetType,
                targetName,
                operator(operatorName),
                LocalDateTime.now(),
                details
        );
    }

    private String contributorLabel(HeritageResource resource) {
        return resource.trackingId() == null ? "Contributor" : "Tracking " + resource.trackingId();
    }

    private String buildSubmissionMetadata(HeritageResource resource) {
        return "Submission ID: " + valueOrFallback(resource.trackingId(), "N/A")
                + " | Status: " + resource.status()
                + " | Created: " + formatDateTime(resource.createdAt());
    }

    private String defaultImage(String url) {
        return hasText(url) ? url : "/review/images/resource-placeholder.svg";
    }

    private String mapReviewStatus(String status) {
        return switch (String.valueOf(status).toUpperCase(Locale.ROOT)) {
            case "PENDING" -> "PENDING_REVIEW";
            case "APPROVED" -> "APPROVED";
            case "REJECTED" -> "REJECTED";
            default -> String.valueOf(status).toUpperCase(Locale.ROOT);
        };
    }

    private String formatDate(LocalDateTime value) {
        return value == null ? "" : DATE_FORMATTER.format(value);
    }

    private String formatDateTime(LocalDateTime value) {
        return value == null ? "" : DATE_TIME_FORMATTER.format(value);
    }

    private LocalDateTime parseDateTime(String value) {
        if (!hasText(value)) {
            return LocalDateTime.MIN;
        }
        String normalized = value.trim().replace('T', ' ');
        try {
            return LocalDateTime.parse(normalized, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        } catch (Exception ignored) {
            try {
                return LocalDateTime.parse(value.trim());
            } catch (Exception ex) {
                return LocalDateTime.MIN;
            }
        }
    }

    private LocalDateTime parseDate(String value) {
        if (!hasText(value)) {
            return LocalDateTime.MIN;
        }
        try {
            return LocalDateTime.parse(value.trim() + "T00:00:00");
        } catch (Exception e) {
            return LocalDateTime.MIN;
        }
    }

    private String operator(String operatorName) {
        return hasText(operatorName) ? operatorName.trim() : SYSTEM_OPERATOR;
    }

    private String requireText(String value, String message) {
        if (!hasText(value)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, message);
        }
        return value.trim();
    }

    private String valueOrFallback(String value, String fallback) {
        return hasText(value) ? value : fallback;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
