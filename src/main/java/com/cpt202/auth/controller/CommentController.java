package com.cpt202.auth.controller;

import com.cpt202.auth.dto.CommentResponse;
import com.cpt202.auth.model.UserRole;
import com.cpt202.auth.service.CommentService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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
    public CommentResponse toggleLike(@PathVariable Long commentId, HttpSession session) {
        return commentService.toggleLike(commentId, currentUserId(session), currentRole(session));
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
