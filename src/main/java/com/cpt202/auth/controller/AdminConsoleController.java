package com.cpt202.auth.controller;

import com.cpt202.auth.dto.ResourceAppealRequest;
import com.cpt202.auth.dto.ResourceAppealSubmissionResponse;
import com.cpt202.auth.dto.MessageThreadSubmissionResponse;
import com.cpt202.auth.dto.admin.AdminActionResponse;
import com.cpt202.auth.dto.admin.AdminArchiveItemResponse;
import com.cpt202.auth.dto.admin.AdminCategoryItemResponse;
import com.cpt202.auth.dto.admin.AdminComplaintDetailResponse;
import com.cpt202.auth.dto.admin.AdminComplaintItemResponse;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminConsoleController {

    private final AdminConsoleService adminConsoleService;

    public AdminConsoleController(AdminConsoleService adminConsoleService) {
        this.adminConsoleService = adminConsoleService;
    }

    @GetMapping("/dashboard")
    public AdminDashboardSummaryResponse getDashboard(HttpSession session) {
        requireAdmin(session);
        return adminConsoleService.getDashboardSummary();
    }

    @GetMapping("/resources/reviews")
    public List<AdminResourceReviewItemResponse> getResourceReviewList(HttpSession session) {
        requireAdmin(session);
        return adminConsoleService.getResourceReviewList();
    }

    @GetMapping("/resources/reviews/{resourceId}")
    public AdminResourceReviewDetailResponse getResourceReviewDetail(@PathVariable("resourceId") Long resourceId, HttpSession session) {
        requireAdmin(session);
        return adminConsoleService.getResourceReviewDetail(resourceId);
    }

    @PostMapping("/resources/reviews/{resourceId}/approve")
    public AdminActionResponse approveResourceReview(@PathVariable("resourceId") Long resourceId, HttpSession session) {
        requireAdmin(session);
        return adminConsoleService.approveResourceReview(resourceId, currentUsername(session));
    }

    @PostMapping("/resources/reviews/{resourceId}/reject")
    public AdminActionResponse rejectResourceReview(@PathVariable("resourceId") Long resourceId,
                                                    @Valid @RequestBody AdminRejectionRequest request,
                                                    HttpSession session) {
        requireAdmin(session);
        return adminConsoleService.rejectResourceReview(resourceId, request.rejectionComments(), currentUsername(session));
    }

    @PostMapping("/resources/reviews/{resourceId}/archive")
    public AdminActionResponse archiveApprovedResource(@PathVariable("resourceId") Long resourceId,
                                                       @Valid @RequestBody AdminRejectionRequest request,
                                                       HttpSession session) {
        requireAdmin(session);
        return adminConsoleService.archiveApprovedResource(resourceId, request.rejectionComments(), currentUsername(session));
    }

    @PostMapping("/resources/reviews/{resourceId}/appeals")
    public ResourceAppealSubmissionResponse submitResourceReviewReply(@PathVariable("resourceId") Long resourceId,
                                                                      @Valid @RequestBody ResourceAppealRequest request,
                                                                      HttpSession session) {
        requireAdmin(session);
        return adminConsoleService.submitResourceReviewReply(resourceId, currentUsername(session), request.content());
    }

    @GetMapping("/categories")
    public List<AdminCategoryItemResponse> getCategories(HttpSession session) {
        requireAdmin(session);
        return adminConsoleService.getCategories();
    }

    @PostMapping("/categories")
    public AdminCategoryItemResponse createCategory(@Valid @RequestBody AdminTaxonomyRequest request, HttpSession session) {
        requireAdmin(session);
        return adminConsoleService.createCategory(request, currentUsername(session));
    }

    @PutMapping("/categories/{categoryId}")
    public AdminCategoryItemResponse updateCategory(@PathVariable("categoryId") Long categoryId,
                                                    @Valid @RequestBody AdminTaxonomyRequest request,
                                                    HttpSession session) {
        requireAdmin(session);
        return adminConsoleService.updateCategory(categoryId, request, currentUsername(session));
    }

    @PostMapping("/categories/{categoryId}/toggle-status")
    public AdminCategoryItemResponse toggleCategoryStatus(@PathVariable("categoryId") Long categoryId, HttpSession session) {
        requireAdmin(session);
        return adminConsoleService.toggleCategoryStatus(categoryId, currentUsername(session));
    }

    @GetMapping("/tags")
    public List<AdminTagItemResponse> getTags(HttpSession session) {
        requireAdmin(session);
        return adminConsoleService.getTags();
    }

    @PostMapping("/tags")
    public AdminTagItemResponse createTag(@Valid @RequestBody AdminTaxonomyRequest request, HttpSession session) {
        requireAdmin(session);
        return adminConsoleService.createTag(request, currentUsername(session));
    }

    @PutMapping("/tags/{tagId}")
    public AdminTagItemResponse updateTag(@PathVariable("tagId") Long tagId,
                                          @Valid @RequestBody AdminTaxonomyRequest request,
                                          HttpSession session) {
        requireAdmin(session);
        return adminConsoleService.updateTag(tagId, request, currentUsername(session));
    }

    @PostMapping("/tags/{tagId}/toggle-status")
    public AdminTagItemResponse toggleTagStatus(@PathVariable("tagId") Long tagId, HttpSession session) {
        requireAdmin(session);
        return adminConsoleService.toggleTagStatus(tagId, currentUsername(session));
    }

    @GetMapping("/archives")
    public List<AdminArchiveItemResponse> getArchives(HttpSession session) {
        requireAdmin(session);
        return adminConsoleService.getArchiveItems();
    }

    @GetMapping("/archives/{archiveId}")
    public AdminArchiveItemResponse getArchiveDetail(@PathVariable("archiveId") Long archiveId, HttpSession session) {
        requireAdmin(session);
        return adminConsoleService.getArchiveDetail(archiveId);
    }

    @PostMapping("/archives/{archiveId}/restore")
    public AdminActionResponse restoreArchive(@PathVariable("archiveId") Long archiveId, HttpSession session) {
        requireAdmin(session);
        return adminConsoleService.restoreArchive(archiveId, currentUsername(session));
    }

    @GetMapping("/history")
    public List<AdminHistoryItemResponse> getHistory(HttpSession session) {
        requireAdmin(session);
        return adminConsoleService.getHistoryItems();
    }

    @GetMapping("/complaints")
    public List<AdminComplaintItemResponse> getComplaints(HttpSession session) {
        requireAdmin(session);
        return adminConsoleService.getComplaintItems();
    }

    @GetMapping("/complaints/{complaintType}/{complaintId}")
    public AdminComplaintDetailResponse getComplaintDetail(@PathVariable("complaintType") String complaintType,
                                                           @PathVariable("complaintId") Long complaintId,
                                                           HttpSession session) {
        requireAdmin(session);
        return adminConsoleService.getComplaintDetail(complaintType, complaintId);
    }

    @PostMapping("/complaints/{complaintType}/{complaintId}/reply")
    public MessageThreadSubmissionResponse replyToComplaint(@PathVariable("complaintType") String complaintType,
                                                            @PathVariable("complaintId") Long complaintId,
                                                            @Valid @RequestBody ResourceAppealRequest request,
                                                            HttpSession session) {
        requireAdmin(session);
        return adminConsoleService.replyToComplaint(complaintType, complaintId, currentUsername(session), request.content());
    }

    @PostMapping("/complaints/resource-report/{complaintId}/reopen")
    public AdminActionResponse reopenReportedResource(@PathVariable("complaintId") Long complaintId,
                                                      HttpSession session) {
        requireAdmin(session);
        return adminConsoleService.reopenReportedResource(complaintId, currentUsername(session));
    }

    @DeleteMapping("/complaints/comment-report/{complaintId}/delete-comment")
    public AdminActionResponse deleteReportedComment(@PathVariable("complaintId") Long complaintId,
                                                     HttpSession session) {
        requireAdmin(session);
        return adminConsoleService.deleteReportedComment(complaintId, currentUsername(session));
    }

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

    private String currentUsername(HttpSession session) {
        Object username = session.getAttribute("username");
        return username instanceof String value && !value.isBlank() ? value : "Admin Console";
    }
}
