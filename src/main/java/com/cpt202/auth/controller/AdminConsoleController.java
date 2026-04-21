package com.cpt202.auth.controller;

import com.cpt202.auth.dto.ResourceAppealRequest;
import com.cpt202.auth.dto.ResourceAppealSubmissionResponse;
import com.cpt202.auth.dto.admin.AdminActionResponse;
import com.cpt202.auth.dto.admin.AdminArchiveItemResponse;
import com.cpt202.auth.dto.admin.AdminCategoryItemResponse;
import com.cpt202.auth.dto.admin.AdminDashboardSummaryResponse;
import com.cpt202.auth.dto.admin.AdminHistoryItemResponse;
import com.cpt202.auth.dto.admin.AdminRejectionRequest;
import com.cpt202.auth.dto.admin.AdminResourceReviewDetailResponse;
import com.cpt202.auth.dto.admin.AdminResourceReviewItemResponse;
import com.cpt202.auth.dto.admin.AdminTagItemResponse;
import com.cpt202.auth.dto.admin.AdminTaxonomyRequest;
import com.cpt202.auth.exception.ApiException;
import com.cpt202.auth.model.UserRole;
import com.cpt202.auth.service.AdminConsoleService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Administrator console APIs.
 */
@RestController
@RequestMapping("/api/admin")
public class AdminConsoleController {

    /**
     * Service that powers the admin console workflows.
     */
    private final AdminConsoleService adminConsoleService;

    public AdminConsoleController(AdminConsoleService adminConsoleService) {
        this.adminConsoleService = adminConsoleService;
    }

    /**
     * Returns the dashboard summary for administrators.
     */
    @GetMapping("/dashboard")
    public AdminDashboardSummaryResponse getDashboard(HttpSession session) {
        requireAdmin(session);
        return adminConsoleService.getDashboardSummary();
    }

    /**
     * Returns the resource moderation queue.
     */
    @GetMapping("/resources/reviews")
    public List<AdminResourceReviewItemResponse> getResourceReviewList(HttpSession session) {
        requireAdmin(session);
        return adminConsoleService.getResourceReviewList();
    }

    /**
     * Returns the details for a resource under review.
     */
    @GetMapping("/resources/reviews/{resourceId}")
    public AdminResourceReviewDetailResponse getResourceReviewDetail(@PathVariable Long resourceId, HttpSession session) {
        requireAdmin(session);
        return adminConsoleService.getResourceReviewDetail(resourceId);
    }

    /**
     * Approves a pending resource review.
     */
    @PostMapping("/resources/reviews/{resourceId}/approve")
    public AdminActionResponse approveResourceReview(@PathVariable Long resourceId, HttpSession session) {
        requireAdmin(session);
        return adminConsoleService.approveResourceReview(resourceId, currentUsername(session));
    }

    /**
     * Rejects a pending resource review with feedback.
     */
    @PostMapping("/resources/reviews/{resourceId}/reject")
    public AdminActionResponse rejectResourceReview(@PathVariable Long resourceId,
                                                    @Valid @RequestBody AdminRejectionRequest request,
                                                    HttpSession session) {
        requireAdmin(session);
        return adminConsoleService.rejectResourceReview(resourceId, request.rejectionComments(), currentUsername(session));
    }

    /**
     * Sends a reply in the resource appeal thread.
     */
    @PostMapping("/resources/reviews/{resourceId}/appeals")
    public ResourceAppealSubmissionResponse submitResourceReviewReply(@PathVariable Long resourceId,
                                                                     @Valid @RequestBody ResourceAppealRequest request,
                                                                     HttpSession session) {
        requireAdmin(session);
        return adminConsoleService.submitResourceReviewReply(resourceId, currentUsername(session), request.content());
    }

    /**
     * Returns the managed category list.
     */
    @GetMapping("/categories")
    public List<AdminCategoryItemResponse> getCategories(HttpSession session) {
        requireAdmin(session);
        return adminConsoleService.getCategories();
    }

    /**
     * Creates a new managed category.
     */
    @PostMapping("/categories")
    public AdminCategoryItemResponse createCategory(@Valid @RequestBody AdminTaxonomyRequest request, HttpSession session) {
        requireAdmin(session);
        return adminConsoleService.createCategory(request, currentUsername(session));
    }

    /**
     * Updates an existing managed category.
     */
    @PutMapping("/categories/{categoryId}")
    public AdminCategoryItemResponse updateCategory(@PathVariable Long categoryId,
                                                    @Valid @RequestBody AdminTaxonomyRequest request,
                                                    HttpSession session) {
        requireAdmin(session);
        return adminConsoleService.updateCategory(categoryId, request, currentUsername(session));
    }

    /**
     * Toggles the active status of a managed category.
     */
    @PostMapping("/categories/{categoryId}/toggle-status")
    public AdminCategoryItemResponse toggleCategoryStatus(@PathVariable Long categoryId, HttpSession session) {
        requireAdmin(session);
        return adminConsoleService.toggleCategoryStatus(categoryId, currentUsername(session));
    }

    /**
     * Returns the managed tag list.
     */
    @GetMapping("/tags")
    public List<AdminTagItemResponse> getTags(HttpSession session) {
        requireAdmin(session);
        return adminConsoleService.getTags();
    }

    /**
     * Creates a new managed tag.
     */
    @PostMapping("/tags")
    public AdminTagItemResponse createTag(@Valid @RequestBody AdminTaxonomyRequest request, HttpSession session) {
        requireAdmin(session);
        return adminConsoleService.createTag(request, currentUsername(session));
    }

    /**
     * Updates an existing managed tag.
     */
    @PutMapping("/tags/{tagId}")
    public AdminTagItemResponse updateTag(@PathVariable Long tagId,
                                          @Valid @RequestBody AdminTaxonomyRequest request,
                                          HttpSession session) {
        requireAdmin(session);
        return adminConsoleService.updateTag(tagId, request, currentUsername(session));
    }

    /**
     * Toggles the active status of a managed tag.
     */
    @PostMapping("/tags/{tagId}/toggle-status")
    public AdminTagItemResponse toggleTagStatus(@PathVariable Long tagId, HttpSession session) {
        requireAdmin(session);
        return adminConsoleService.toggleTagStatus(tagId, currentUsername(session));
    }

    /**
     * Returns the archived resource list.
     */
    @GetMapping("/archives")
    public List<AdminArchiveItemResponse> getArchives(HttpSession session) {
        requireAdmin(session);
        return adminConsoleService.getArchiveItems();
    }

    /**
     * Returns a single archive record.
     */
    @GetMapping("/archives/{archiveId}")
    public AdminArchiveItemResponse getArchiveDetail(@PathVariable Long archiveId, HttpSession session) {
        requireAdmin(session);
        return adminConsoleService.getArchiveDetail(archiveId);
    }

    /**
     * Restores an archived resource.
     */
    @PostMapping("/archives/{archiveId}/restore")
    public AdminActionResponse restoreArchive(@PathVariable Long archiveId, HttpSession session) {
        requireAdmin(session);
        return adminConsoleService.restoreArchive(archiveId, currentUsername(session));
    }

    /**
     * Returns the recent admin activity history.
     */
    @GetMapping("/history")
    public List<AdminHistoryItemResponse> getHistory(HttpSession session) {
        requireAdmin(session);
        return adminConsoleService.getHistoryItems();
    }

    /**
     * Ensures the current session belongs to an administrator.
     */
    private void requireAdmin(HttpSession session) {
        Object roleValue = session.getAttribute("role");
        if (roleValue == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Please log in to continue.");
        }
        UserRole role = UserRole.fromDatabaseValue(String.valueOf(roleValue));
        if (!role.canAccessAdmin()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Administrator access is required.");
        }
    }

    /**
     * Returns the current operator name for admin actions.
     */
    private String currentUsername(HttpSession session) {
        Object username = session.getAttribute("username");
        return username instanceof String value && !value.isBlank() ? value : "Admin Console";
    }
}
