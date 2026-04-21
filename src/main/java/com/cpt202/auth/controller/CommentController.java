package com.cpt202.auth.controller;

import com.cpt202.auth.dto.AddCommentRequest;
import com.cpt202.auth.dto.CommentResponse;
import com.cpt202.auth.model.UserRole;
import com.cpt202.auth.service.CommentService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Comment APIs.
 */
@RestController
@RequestMapping("/api/comments")
public class CommentController {

    /**
     * Comment business service.
     */
    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    /**
     * Toggles the current user's like on a comment.
     */
    @PostMapping("/{commentId}/like")
    public CommentResponse toggleLike(@PathVariable Long commentId, HttpSession session) {
        return commentService.toggleLike(commentId, currentUserId(session), currentRole(session));
    }

    /**
     * Adds a reply to a root comment.
     */
    @PostMapping("/{parentId}/reply")
    public CommentResponse replyToComment(
            @PathVariable Long parentId,
            @Valid @RequestBody AddCommentRequest request,
            HttpSession session
    ) {
        return commentService.replyToComment(parentId, currentUserId(session), currentRole(session), request.content());
    }

    /**
     * Updates the current user's own comment.
     */
    @PutMapping("/{commentId}")
    public CommentResponse updateComment(
            @PathVariable Long commentId,
            @Valid @RequestBody AddCommentRequest request,
            HttpSession session
    ) {
        return commentService.updateComment(commentId, currentUserId(session), currentRole(session), request.content());
    }

    /**
     * Deletes the current user's own comment.
     */
    @DeleteMapping("/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteComment(@PathVariable Long commentId, HttpSession session) {
        commentService.deleteComment(commentId, currentUserId(session), currentRole(session));
    }

    /**
     * Returns the current session user id.
     */
    private Long currentUserId(HttpSession session) {
        Object userId = session.getAttribute("userId");
        return userId instanceof Long ? (Long) userId : null;
    }

    /**
     * Returns the current session role.
     */
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
