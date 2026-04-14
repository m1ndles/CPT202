package com.cpt202.auth.controller;

import com.cpt202.auth.dto.AddCommentRequest;
import com.cpt202.auth.dto.CommentResponse;
import com.cpt202.auth.dto.DraftAttachmentResponse;
import com.cpt202.auth.dto.PageResponse;
import com.cpt202.auth.dto.ResourceDetail;
import com.cpt202.auth.dto.ResourceSubmissionDto;
import com.cpt202.auth.dto.ResourceSummary;
import com.cpt202.auth.exception.ApiException;
import com.cpt202.auth.model.HeritageResource;
import com.cpt202.auth.model.UserRole;
import com.cpt202.auth.service.CommentService;
import com.cpt202.auth.service.DraftAttachmentService;
import com.cpt202.auth.service.ResourceService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Resource APIs.
 */
@RestController
@RequestMapping("/api/resources")
public class ResourceController {

    private final ResourceService resourceService;
    private final CommentService commentService;
    private final DraftAttachmentService draftAttachmentService;

    public ResourceController(ResourceService resourceService,
                              CommentService commentService,
                              DraftAttachmentService draftAttachmentService) {
        this.resourceService = resourceService;
        this.commentService = commentService;
        this.draftAttachmentService = draftAttachmentService;
    }

    @GetMapping
    public PageResponse<ResourceSummary> getResources(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "") String category,
            @RequestParam(defaultValue = "") String place,
            @RequestParam(defaultValue = "newest") String sort,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "6") int size
    ) {
        return resourceService.getResources(keyword, category, place, sort, page, size);
    }

    @GetMapping("/categories")
    public List<String> getCategories() {
        return resourceService.getCategories();
    }

    @GetMapping("/places")
    public List<String> getPlaces() {
        return resourceService.getPlaces();
    }

    @GetMapping("/{resourceId}")
    public ResourceDetail getResource(@PathVariable Long resourceId) {
        return resourceService.getResource(resourceId);
    }

    @PostMapping("/{resourceId}/view")
    public Map<String, Integer> incrementView(@PathVariable Long resourceId) {
        return Map.of("viewCount", resourceService.incrementView(resourceId));
    }

    @GetMapping("/{resourceId}/comments")
    public PageResponse<CommentResponse> getComments(
            @PathVariable Long resourceId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpSession session
    ) {
        return commentService.getComments(resourceId, currentUserId(session), page, size);
    }

    @PostMapping("/{resourceId}/comments")
    public CommentResponse addComment(
            @PathVariable Long resourceId,
            @Valid @RequestBody AddCommentRequest request,
            HttpSession session
    ) {
        return commentService.addComment(resourceId, currentUserId(session), currentRole(session), request.content());
    }

    @PostMapping("/submit")
    public ResponseEntity<Map<String, Object>> submitResource(@RequestBody ResourceSubmissionDto request,
                                                              HttpSession session) {
        requireUploadPermission(session);
        HeritageResource resource = resourceService.submitResource(request);
        Object email = session.getAttribute("email");
        if (email instanceof String emailValue) {
            resourceService.sendSubmissionConfirmation(emailValue, resource);
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of(
                        "message", "Resource submitted successfully.",
                        "id", resource.id(),
                        "status", resource.status(),
                        "trackingId", resource.trackingId()
                ));
    }

    @PostMapping("/draft")
    public ResponseEntity<Map<String, Object>> createDraft(@RequestBody ResourceSubmissionDto request,
                                                           HttpSession session) {
        requireUploadPermission(session);
        HeritageResource resource = resourceService.createDraft(request);
        String draftId = "DRAFT-" + resource.id();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of(
                        "message", "Draft created successfully.",
                        "id", resource.id(),
                        "draftId", draftId,
                        "status", resource.status(),
                        "savedAt", java.time.LocalDateTime.now().toString()
                ));
    }

    @GetMapping("/draft/{resourceId}")
    public Map<String, Object> getDraft(@PathVariable Long resourceId,
                                        HttpSession session) {
        requireUploadPermission(session);
        HeritageResource resource = resourceService.getDraft(resourceId);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", resource.id());
        response.put("title", resource.title());
        response.put("description", resource.description());
        response.put("category", resource.category());
        response.put("period", resource.period() == null ? "" : resource.period());
        response.put("place", resource.place());
        response.put("tags", resourceService.getResourceTags(resource.id()));
        response.put("thumbnail", resource.thumbnail() == null ? "" : resource.thumbnail());
        response.put("copyright", resource.copyright() == null ? "" : resource.copyright());
        response.put("trackingId", resource.trackingId() == null ? "" : resource.trackingId());
        response.put("status", resource.status());
        response.put("savedAt", java.time.LocalDateTime.now().toString());
        response.put("attachments", draftAttachmentService.getDraftAttachments(resourceId));
        return response;
    }

    @PostMapping(value = "/draft/{resourceId}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DraftAttachmentResponse> uploadDraftAttachment(
            @PathVariable Long resourceId,
            @RequestPart("file") MultipartFile file,
            HttpSession session
    ) {
        requireUploadPermission(session);
        DraftAttachmentResponse attachment = draftAttachmentService.uploadDraftAttachment(resourceId, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(attachment);
    }

    @DeleteMapping("/draft/{resourceId}/attachments/{attachmentId}")
    public ResponseEntity<Map<String, String>> deleteDraftAttachment(
            @PathVariable Long resourceId,
            @PathVariable Long attachmentId,
            HttpSession session
    ) {
        requireUploadPermission(session);
        draftAttachmentService.removeDraftAttachment(resourceId, attachmentId);
        return ResponseEntity.ok(Map.of("message", "Attachment removed."));
    }

    @GetMapping("/files/{storedName}")
    public ResponseEntity<Resource> getAttachment(@PathVariable String storedName) {
        Resource resource = draftAttachmentService.loadAttachment(storedName);
        String contentType = draftAttachmentService.detectContentType(storedName);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                .contentType(MediaType.parseMediaType(contentType == null ? "application/octet-stream" : contentType))
                .body(resource);
    }

    private Long currentUserId(HttpSession session) {
        Object userId = session.getAttribute("userId");
        return userId instanceof Long ? (Long) userId : null;
    }

    private UserRole currentRole(HttpSession session) {
        Object role = session.getAttribute("role");
        if (role == null) return null;
        try {
            return UserRole.fromDatabaseValue(String.valueOf(role));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private void requireUploadPermission(HttpSession session) {
        UserRole role = currentRole(session);
        if (role == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Please log in to continue.");
        }
        if (!role.canUpload()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Your current role cannot submit new resources.");
        }
    }
}
