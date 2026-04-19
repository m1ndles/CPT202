package com.cpt202.auth.controller;

import com.cpt202.auth.dto.AddCommentRequest;
import com.cpt202.auth.dto.CommentResponse;
import com.cpt202.auth.dto.DraftAttachmentResponse;
import com.cpt202.auth.dto.MessageThreadSubmissionResponse;
import com.cpt202.auth.dto.MyResourceItemResponse;
import com.cpt202.auth.dto.PageResponse;
import com.cpt202.auth.dto.ResourceAppealRequest;
import com.cpt202.auth.dto.ResourceAppealSubmissionResponse;
import com.cpt202.auth.dto.ResourceRevisionCancelRequest;
import com.cpt202.auth.dto.ResourceDetail;
import com.cpt202.auth.dto.ResourceFavoriteResponse;
import com.cpt202.auth.dto.ResourceSubmissionDto;
import com.cpt202.auth.dto.ResourceSummary;
import com.cpt202.auth.exception.ApiException;
import com.cpt202.auth.model.HeritageResource;
import com.cpt202.auth.model.UserAccount;
import com.cpt202.auth.model.UserRole;
import com.cpt202.auth.repository.UserRepository;
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
    private final UserRepository userRepository;

    public ResourceController(ResourceService resourceService,
                              CommentService commentService,
                              DraftAttachmentService draftAttachmentService,
                              UserRepository userRepository) {
        this.resourceService = resourceService;
        this.commentService = commentService;
        this.draftAttachmentService = draftAttachmentService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public PageResponse<ResourceSummary> getResources(
            @RequestParam(name = "keyword", defaultValue = "") String keyword,
            @RequestParam(name = "category", defaultValue = "") String category,
            @RequestParam(name = "place", defaultValue = "") String place,
            @RequestParam(name = "sort", defaultValue = "newest") String sort,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "size", defaultValue = "6") int size
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

    @GetMapping("/mine")
    public List<MyResourceItemResponse> getMyResources(
            @RequestParam(name = "status", defaultValue = "") String status,
            HttpSession session
    ) {
        UserAccount user = requireUploadPermission(session);
        return resourceService.getMyResources(user.id(), status);
    }

    @GetMapping("/favorites")
    public List<ResourceSummary> getMyFavoriteResources(HttpSession session) {
        UserAccount user = requireRegisteredUser(session);
        return resourceService.getMyFavoriteResources(user.id());
    }

    @GetMapping("/{resourceId}")
    public ResourceDetail getResource(@PathVariable("resourceId") Long resourceId,
                                      HttpSession session) {
        return resourceService.getResource(resourceId, currentUserId(session));
    }

    @DeleteMapping("/{resourceId}")
    public ResponseEntity<Map<String, String>> deleteOwnedResource(@PathVariable("resourceId") Long resourceId,
                                                                   HttpSession session) {
        UserAccount user = requireUploadPermission(session);
        resourceService.deleteOwnedResource(resourceId, user.id());
        return ResponseEntity.ok(Map.of("message", "Resource deleted successfully."));
    }

    @PostMapping("/{resourceId}/favorite")
    public ResourceFavoriteResponse toggleFavorite(@PathVariable("resourceId") Long resourceId,
                                                   HttpSession session) {
        UserAccount user = requireRegisteredUser(session);
        return resourceService.toggleFavorite(resourceId, user.id());
    }

    @PostMapping("/{resourceId}/view")
    public Map<String, Integer> incrementView(@PathVariable("resourceId") Long resourceId) {
        return Map.of("viewCount", resourceService.incrementView(resourceId));
    }

    @GetMapping("/{resourceId}/comments")
    public PageResponse<CommentResponse> getComments(
            @PathVariable("resourceId") Long resourceId,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            HttpSession session
    ) {
        return commentService.getComments(resourceId, currentUserId(session), page, size);
    }

    @PostMapping("/{resourceId}/comments")
    public CommentResponse addComment(
            @PathVariable("resourceId") Long resourceId,
            @Valid @RequestBody AddCommentRequest request,
            HttpSession session
    ) {
        return commentService.addComment(resourceId, currentUserId(session), currentRole(session), request.content());
    }

    @PostMapping("/submit")
    public ResponseEntity<Map<String, Object>> submitResource(@RequestBody ResourceSubmissionDto request,
                                                              HttpSession session) {
        UserAccount user = requireUploadPermission(session);
        HeritageResource resource = resourceService.submitResource(request, user.id(), user.username());
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
        UserAccount user = requireUploadPermission(session);
        HeritageResource resource = resourceService.createDraft(request, user.id(), user.username());
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
    public Map<String, Object> getDraft(@PathVariable("resourceId") Long resourceId,
                                        HttpSession session) {
        UserAccount user = requireUploadPermission(session);
        HeritageResource resource = resourceService.getOwnedDraft(resourceId, user.id());
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", resource.id());
        response.put("title", resource.title());
        response.put("titleEn", resource.titleEn() == null ? "" : resource.titleEn());
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
        response.put("rejectionComments", resourceService.getRevisionFeedback(resourceId, user.id()));
        response.put("appealMessages", resourceService.getAppealMessages(resourceId, user.id()));
        response.put("canSendAppeal", resourceService.canSendAppeal(resourceId, user.id()));
        response.put("attachments", draftAttachmentService.getDraftAttachments(resourceId));
        return response;
    }

    @PostMapping("/{resourceId}/revision-draft")
    public Map<String, Object> createRevisionDraft(@PathVariable("resourceId") Long resourceId,
                                                   HttpSession session) {
        UserAccount user = requireUploadPermission(session);
        HeritageResource resource = resourceService.createRevisionDraft(resourceId, user.id(), user.username());
        return Map.of(
                "message", "Revision draft prepared.",
                "id", resource.id(),
                "status", resource.status()
        );
    }

    @PostMapping("/{resourceId}/revision-cancel")
    public Map<String, Object> cancelRevisionDraft(@PathVariable("resourceId") Long resourceId,
                                                   @Valid @RequestBody ResourceRevisionCancelRequest request,
                                                   HttpSession session) {
        UserAccount user = requireUploadPermission(session);
        HeritageResource resource = resourceService.cancelRevisionDraft(resourceId, user.id(), user.username(), request);
        return Map.of(
                "message", "Revision cancelled and the original state has been restored.",
                "id", resource.id(),
                "status", resource.status()
        );
    }

    @PostMapping("/{resourceId}/appeals")
    public ResourceAppealSubmissionResponse submitAppeal(
            @PathVariable("resourceId") Long resourceId,
            @Valid @RequestBody ResourceAppealRequest request,
            HttpSession session
    ) {
        UserAccount user = requireUploadPermission(session);
        return resourceService.submitAppeal(resourceId, user.id(), user.username(), request.content());
    }

    @PostMapping("/{resourceId}/reports")
    public MessageThreadSubmissionResponse submitReport(
            @PathVariable("resourceId") Long resourceId,
            @Valid @RequestBody ResourceAppealRequest request,
            HttpSession session
    ) {
        UserAccount user = requireRegisteredUser(session);
        return resourceService.submitReport(resourceId, user.id(), user.username(), request.content());
    }

    @PostMapping(value = "/draft/{resourceId}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DraftAttachmentResponse> uploadDraftAttachment(
            @PathVariable("resourceId") Long resourceId,
            @RequestPart("file") MultipartFile file,
            HttpSession session
    ) {
        UserAccount user = requireUploadPermission(session);
        resourceService.getOwnedEditableDraft(resourceId, user.id());
        DraftAttachmentResponse attachment = draftAttachmentService.uploadDraftAttachment(resourceId, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(attachment);
    }

    @DeleteMapping("/draft/{resourceId}/attachments/{attachmentId}")
    public ResponseEntity<Map<String, String>> deleteDraftAttachment(
            @PathVariable("resourceId") Long resourceId,
            @PathVariable("attachmentId") Long attachmentId,
            HttpSession session
    ) {
        UserAccount user = requireUploadPermission(session);
        resourceService.getOwnedEditableDraft(resourceId, user.id());
        draftAttachmentService.removeDraftAttachment(resourceId, attachmentId);
        return ResponseEntity.ok(Map.of("message", "Attachment removed."));
    }

    @GetMapping("/files/{storedName}")
    public ResponseEntity<Resource> getAttachment(@PathVariable("storedName") String storedName) {
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

    private UserAccount requireUploadPermission(HttpSession session) {
        Long userId = currentUserId(session);
        if (userId == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Please log in to continue.");
        }
        UserAccount user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Please log in to continue."));
        UserRole role = user.role();
        session.setAttribute("role", role.name());
        session.setAttribute("username", user.username());
        session.setAttribute("email", user.email());
        if (!role.canUpload()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Your current role cannot submit new resources.");
        }
        return user;
    }

    private UserAccount requireRegisteredUser(HttpSession session) {
        Object roleValue = session.getAttribute("role");
        if (roleValue == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Please log in to continue.");
        }

        Long userId = currentUserId(session);
        if (userId == null) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Please log in with a registered account to manage favorites.");
        }

        return userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Please log in to continue."));
    }
}
