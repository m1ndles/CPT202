package com.cpt202.auth.controller;

import com.cpt202.auth.dto.ContributorApplicationRequest;
import com.cpt202.auth.dto.ContributorApplicationResponse;
import com.cpt202.auth.dto.ContributorApplicationSummaryResponse;
import com.cpt202.auth.dto.MessageThreadSubmissionResponse;
import com.cpt202.auth.dto.ResourceAppealRequest;
import com.cpt202.auth.exception.ApiException;
import com.cpt202.auth.model.UserRole;
import com.cpt202.auth.service.ContributorApplicationService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Contributor application APIs for applicants and administrators.
 */
@RestController
@RequestMapping("/api/contributor-applications")
public class ContributorApplicationController {

    private final ContributorApplicationService contributorApplicationService;

    public ContributorApplicationController(ContributorApplicationService contributorApplicationService) {
        this.contributorApplicationService = contributorApplicationService;
    }

    /**
     * Returns the current user's latest contributor application.
     */
    @GetMapping("/current")
    public ContributorApplicationResponse getCurrentApplication(HttpSession session) {
        return contributorApplicationService.getCurrentApplication(currentUserId(session));
    }

    /**
     * Returns the current user's contributor application history.
     */
    @GetMapping("/mine")
    public List<ContributorApplicationSummaryResponse> getMyApplications(HttpSession session) {
        return contributorApplicationService.getMyApplications(currentUserId(session));
    }

    /**
     * Submits a new contributor application with an optional supporting file.
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ContributorApplicationResponse> submitApplication(
            @Valid @RequestPart("application") ContributorApplicationRequest request,
            @RequestPart(value = "file", required = false) MultipartFile file,
            HttpSession session
    ) {
        ContributorApplicationResponse response = contributorApplicationService.submit(currentUserId(session), request, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Sends an appeal message for the current rejected application.
     */
    @PostMapping("/current/appeals")
    public MessageThreadSubmissionResponse submitAppeal(@Valid @RequestBody ResourceAppealRequest request,
                                                        HttpSession session) {
        return contributorApplicationService.submitAppeal(currentUserId(session), request.content());
    }

    /**
     * Returns pending contributor applications for administrators.
     */
    @GetMapping("/admin/pending")
    public List<ContributorApplicationSummaryResponse> getPendingApplications(HttpSession session) {
        requireAdmin(session);
        return contributorApplicationService.getPendingApplications();
    }

    /**
     * Returns a contributor application detail for administrator review.
     */
    @GetMapping("/admin/{applicationId}")
    public ContributorApplicationResponse getApplicationDetail(@PathVariable("applicationId") Long applicationId, HttpSession session) {
        requireAdmin(session);
        return contributorApplicationService.getApplicationDetail(applicationId);
    }

    /**
     * Approves a pending contributor application.
     */
    @PostMapping("/admin/{applicationId}/approve")
    public Map<String, Object> approve(@PathVariable("applicationId") Long applicationId, HttpSession session) {
        requireAdmin(session);
        ContributorApplicationResponse application = contributorApplicationService.approve(applicationId, currentUsername(session));
        return Map.of(
                "message", "Contributor application approved. The user can now submit heritage resources.",
                "application", application
        );
    }

    /**
     * Rejects a pending contributor application with comments.
     */
    @PostMapping("/admin/{applicationId}/reject")
    public Map<String, Object> reject(@PathVariable("applicationId") Long applicationId,
                                      @RequestParam("comments") String comments,
                                      HttpSession session) {
        requireAdmin(session);
        ContributorApplicationResponse application = contributorApplicationService.reject(applicationId, comments, currentUsername(session));
        return Map.of(
                "message", "Contributor application rejected.",
                "application", application
        );
    }

    /**
     * Sends an administrator reply in a contributor application appeal thread.
     */
    @PostMapping("/admin/{applicationId}/appeals")
    public MessageThreadSubmissionResponse replyToAppeal(@PathVariable("applicationId") Long applicationId,
                                                         @Valid @RequestBody ResourceAppealRequest request,
                                                         HttpSession session) {
        requireAdmin(session);
        return contributorApplicationService.replyToAppeal(applicationId, currentUsername(session), request.content());
    }

    /**
     * Returns the current session user id.
     */
    private Long currentUserId(HttpSession session) {
        Object userId = session.getAttribute("userId");
        return userId instanceof Long ? (Long) userId : null;
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
     * Returns the display name used for administrator appeal replies.
     */
    private String currentUsername(HttpSession session) {
        Object username = session.getAttribute("username");
        return username instanceof String value && !value.isBlank() ? value : "Admin Console";
    }
}
