package com.cpt202.auth.controller;

import com.cpt202.auth.dto.CommentResponse;
import com.cpt202.auth.dto.MessageThreadSubmissionResponse;
import com.cpt202.auth.dto.ResourceAppealRequest;
import com.cpt202.auth.model.UserRole;
import com.cpt202.auth.service.CommentService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Comment APIs.
 */
@RestController
@RequestMapping("/api/comments")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @PostMapping("/{commentId}/like")
    public CommentResponse toggleLike(@PathVariable("commentId") Long commentId, HttpSession session) {
        return commentService.toggleLike(commentId, currentUserId(session), currentRole(session));
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<Map<String, String>> deleteComment(@PathVariable("commentId") Long commentId, HttpSession session) {
        commentService.deleteComment(commentId, currentUserId(session), currentRole(session));
        return ResponseEntity.ok(Map.of("message", "Comment deleted."));
    }

    @PostMapping("/{commentId}/reports")
    public MessageThreadSubmissionResponse reportComment(@PathVariable("commentId") Long commentId,
                                                         @Valid @RequestBody ResourceAppealRequest request,
                                                         HttpSession session) {
        return commentService.submitReport(
                commentId,
                currentUserId(session),
                currentRole(session),
                currentUsername(session),
                request.content()
        );
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

    private String currentUsername(HttpSession session) {
        Object username = session.getAttribute("username");
        return username instanceof String value ? value : null;
    }
}
