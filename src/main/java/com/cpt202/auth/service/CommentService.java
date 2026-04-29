package com.cpt202.auth.service;

import com.cpt202.auth.dto.CommentResponse;
import com.cpt202.auth.dto.MessageThreadSubmissionResponse;
import com.cpt202.auth.dto.PageResponse;
import com.cpt202.auth.exception.ApiException;
import com.cpt202.auth.model.UserRole;
import com.cpt202.auth.repository.CommentRepository;
import com.cpt202.auth.repository.CommentRepository.CommentViewRow;
import com.cpt202.auth.repository.CommentReportRepository;
import com.cpt202.auth.repository.ResourceRepository;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Comment business logic.
 */
@Service
public class CommentService {

    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 50;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final CommentRepository commentRepository;
    private final CommentReportRepository commentReportRepository;
    private final ResourceRepository resourceRepository;

    public CommentService(CommentRepository commentRepository,
                          CommentReportRepository commentReportRepository,
                          ResourceRepository resourceRepository) {
        this.commentRepository = commentRepository;
        this.commentReportRepository = commentReportRepository;
        this.resourceRepository = resourceRepository;
    }

    public PageResponse<CommentResponse> getComments(Long resourceId, Long userId, int page, int size) {
        ensureResourceExists(resourceId);

        int safePage = normalizePage(page);
        int safeSize = normalizeSize(size);
        long totalItems = commentRepository.countByResourceId(resourceId);
        int totalPages = totalItems == 0 ? 0 : (int) Math.ceil((double) totalItems / safeSize);
        int offset = (safePage - 1) * safeSize;

        List<CommentResponse> content = commentRepository.findByResourceId(resourceId, userId, offset, safeSize)
                .stream()
                .map(this::toResponse)
                .toList();

        return new PageResponse<>(content, safePage, safeSize, totalItems, totalPages);
    }

    @Transactional
    public CommentResponse addComment(Long resourceId, Long userId, UserRole role, String content) {
        ensureCommentPermission(userId, role);
        ensureResourceExists(resourceId);

        long commentId = commentRepository.create(resourceId, userId, content.trim());
        CommentViewRow row = commentRepository.findViewById(commentId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Comment not found."));
        return toResponse(row);
    }

    @Transactional
    public CommentResponse toggleLike(Long commentId, Long userId, UserRole role) {
        ensureLikePermission(userId, role);

        commentRepository.findViewById(commentId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Comment not found."));

        if (commentRepository.isLikedByUser(commentId, userId)) {
            commentRepository.removeLike(commentId, userId);
        } else {
            commentRepository.addLike(commentId, userId);
        }

        CommentViewRow updated = commentRepository.findViewById(commentId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Comment not found."));
        return toResponse(updated);
    }

    @Transactional
    public void deleteComment(Long commentId, Long userId, UserRole role) {
        if (role == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Please log in to continue.");
        }
        if (userId == null) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You do not have permission to delete comments.");
        }

        CommentViewRow comment = commentRepository.findViewById(commentId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Comment not found."));

        boolean adminCanDelete = role == UserRole.ADMIN;
        boolean contributorOwnComment = role == UserRole.CONTRIBUTOR && userId.equals(comment.userId());

        if (!adminCanDelete && !contributorOwnComment) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You do not have permission to delete this comment.");
        }

        commentRepository.delete(commentId);
    }

    /**
     * Opens or appends to a report thread for a comment.
     */
    @Transactional
    public MessageThreadSubmissionResponse submitReport(Long commentId,
                                                        Long userId,
                                                        UserRole role,
                                                        String username,
                                                        String content) {
        ensureLikePermission(userId, role);
        CommentViewRow comment = commentRepository.findViewById(commentId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Comment not found."));

        String normalizedContent = content == null ? "" : content.trim();
        if (normalizedContent.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Report content is required.");
        }
        if (normalizedContent.length() > 1000) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Report content must be 1000 characters or fewer.");
        }

        LocalDateTime now = LocalDateTime.now();
        CommentReportRepository.CommentReportThreadRecord thread = commentReportRepository
                .findByCommentIdAndReporterUserId(comment.id(), userId)
                .orElseGet(() -> {
                    Long threadId = commentReportRepository.createThread(
                            comment.id(),
                            userId,
                            normalizeUsername(username),
                            "OPEN",
                            now
                    );
                    return commentReportRepository.findById(threadId)
                            .orElseThrow(() -> new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to create report thread."));
                });

        commentReportRepository.insertMessage(
                thread.id(),
                "USER",
                normalizeUsername(username),
                normalizedContent,
                now
        );
        commentReportRepository.updateThreadStatus(thread.id(), "OPEN", now);

        return new MessageThreadSubmissionResponse(
                "Comment report sent to the admin team.",
                commentReportRepository.findMessagesByThreadId(thread.id())
        );
    }

    private void ensureResourceExists(Long resourceId) {
        if (resourceRepository.findById(resourceId).isEmpty()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Resource not found.");
        }
    }

    private void ensureCommentPermission(Long userId, UserRole role) {
        if (role == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Please log in to continue.");
        }
        if (userId == null || !role.canComment()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You do not have permission to comment.");
        }
    }

    private void ensureLikePermission(Long userId, UserRole role) {
        if (role == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Please log in to continue.");
        }
        if (userId == null || role == UserRole.GUEST) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Please use a registered account to like comments.");
        }
    }

    private CommentResponse toResponse(CommentViewRow row) {
        boolean canDelete = false;
        UserRole viewerRole = row.viewerRole() == null ? null : UserRole.fromDatabaseValue(row.viewerRole());
        if (viewerRole == UserRole.ADMIN) {
            canDelete = true;
        } else if (viewerRole == UserRole.CONTRIBUTOR && row.viewerUserId() != null) {
            canDelete = row.viewerUserId().equals(row.userId());
        }

        return new CommentResponse(
                row.id(),
                row.username(),
                UserRole.fromDatabaseValue(row.role()).label(),
                row.content(),
                formatDateTime(row.createdAt()),
                row.likes(),
                row.likedByMe(),
                canDelete
        );
    }

    private int normalizePage(int page) {
        return Math.max(page, 1);
    }

    private int normalizeSize(int size) {
        if (size <= 0) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(size, MAX_PAGE_SIZE);
    }

    private String formatDateTime(java.time.LocalDateTime value) {
        return value == null ? "" : DATE_TIME_FORMATTER.format(value);
    }

    private String normalizeUsername(String username) {
        return username == null || username.isBlank() ? "Registered User" : username.trim();
    }
}
