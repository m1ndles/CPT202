package com.cpt202.auth.controller;

import com.cpt202.auth.dto.AddCommentRequest;
import com.cpt202.auth.dto.CommentResponse;
import com.cpt202.auth.dto.PageResponse;
import com.cpt202.auth.dto.ResourceDetail;
import com.cpt202.auth.dto.ResourceSummary;
import com.cpt202.auth.model.UserRole;
import com.cpt202.auth.service.CommentService;
import com.cpt202.auth.service.ResourceService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Resource APIs.
 */
@RestController
@RequestMapping("/api/resources")
public class ResourceController {

    private final ResourceService resourceService;
    private final CommentService commentService;

    public ResourceController(ResourceService resourceService, CommentService commentService) {
        this.resourceService = resourceService;
        this.commentService = commentService;
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
}
