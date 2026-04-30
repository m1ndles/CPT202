package com.cpt202.auth.service;

import com.cpt202.auth.dto.CommentResponse;
import com.cpt202.auth.dto.MessageThreadSubmissionResponse;
import com.cpt202.auth.dto.PageResponse;
import com.cpt202.auth.exception.ApiException;
import com.cpt202.auth.model.UserRole;
import com.cpt202.auth.repository.CommentReportRepository;
import com.cpt202.auth.repository.CommentRepository;
import com.cpt202.auth.repository.CommentRepository.CommentViewRow;
import com.cpt202.auth.repository.ResourceRepository;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
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

    /**
     * Returns paged root comments and their second-level replies for a resource.
     */
    public PageResponse<CommentResponse> getComments(Long resourceId, Long userId, UserRole role, int page, int size) {
        ensureResourceExists(resourceId);

        int safePage = normalizePage(page);
        int safeSize = normalizeSize(size);
        long totalItems = commentRepository.countRootsByResourceId(resourceId);
        int totalPages = totalItems == 0 ? 0 : (int) Math.ceil((double) totalItems / safeSize);
        int offset = (safePage - 1) * safeSize;

        List<CommentViewRow> roots = commentRepository.findRootsByResourceId(resourceId, userId, offset, safeSize);
        List<Long> rootIds = roots.stream().map(CommentViewRow::id).toList();
        List<CommentViewRow> replies = commentRepository.findRepliesByParentIds(rootIds, userId);
        Map<Long, List<CommentViewRow>> repliesByParent = replies.stream()
                .filter(reply -> reply.parentId() != null)
                .collect(Collectors.groupingBy(CommentViewRow::parentId));

        List<CommentResponse> content = new ArrayList<>();
        for (CommentViewRow root : roots) {
            List<CommentViewRow> childReplies = repliesByParent.getOrDefault(root.id(), List.of());
            content.add(toResponse(root, userId, role, childReplies.size()));
            for (CommentViewRow reply : childReplies) {
                content.add(toResponse(reply, userId, role, 0));
            }
        }

        return new PageResponse<>(content, safePage, safeSize, totalItems, totalPages);
    }

    @Transactional
    public CommentResponse addComment(Long resourceId, Long userId, UserRole role, String content) {
        ensureCommentPermission(userId, role);
        ensureResourceExists(resourceId);

        long commentId = commentRepository.create(resourceId, userId, null, content.trim());
        CommentViewRow row = commentRepository.findViewById(commentId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Comment not found."));
        return toResponse(row, userId, role, 0);
    }

    /**
     * Creates a second-level reply under a root comment.
     */
    @Transactional
    public CommentResponse replyToComment(Long parentId, Long userId, UserRole role, String content) {
        ensureCommentPermission(userId, role);

        CommentViewRow parent = commentRepository.findViewById(parentId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Comment not found."));
        if (parent.parentId() != null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Replies cannot be nested beyond 2 levels.");
        }
        if (isDeleted(parent)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Cannot reply to a deleted comment.");
        }

        long commentId = commentRepository.create(parent.resourceId(), userId, parentId, content.trim());
        CommentViewRow row = commentRepository.findViewById(commentId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Comment not found."));
        return toResponse(row, userId, role, 0);
    }

    /**
     * Updates a comment owned by the current user.
     */
    @Transactional
    public CommentResponse updateComment(Long commentId, Long userId, UserRole role, String content) {
        ensureCommentPermission(userId, role);

        CommentViewRow row = commentRepository.findViewById(commentId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Comment not found."));
        if (!row.userId().equals(userId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You can only edit your own comments.");
        }
        if (isDeleted(row)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Deleted comments cannot be edited.");
        }

        commentRepository.updateContent(commentId, content.trim());
        CommentViewRow updated = commentRepository.findViewById(commentId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Comment not found."));
        return toResponseWithLookup(updated, userId, role);
    }

    @Transactional
    public CommentResponse toggleLike(Long commentId, Long userId, UserRole role) {
        ensureRegisteredCommentUser(userId, role, "Please use a registered account to like comments.");

        CommentViewRow existing = commentRepository.findViewById(commentId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Comment not found."));
        if (isDeleted(existing)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Cannot like a deleted comment.");
        }

        if (commentRepository.isLikedByUser(commentId, userId)) {
            commentRepository.removeLike(commentId, userId);
        } else {
            commentRepository.addLike(commentId, userId);
        }

        CommentViewRow updated = commentRepository.findViewById(commentId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Comment not found."));
        return toResponseWithLookup(updated, userId, role);
    }

    @Transactional
    public void deleteComment(Long commentId, Long userId, UserRole role) {
        ensureCommentPermission(userId, role);

        CommentViewRow row = commentRepository.findViewById(commentId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Comment not found."));
        boolean owner = row.userId().equals(userId);
        boolean admin = role == UserRole.ADMIN;
        if (!owner && !admin) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You do not have permission to delete this comment.");
        }
        if (isDeleted(row)) {
            return;
        }

        commentRepository.softDelete(commentId);
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
        ensureRegisteredCommentUser(userId, role, "Please use a registered account to report comments.");
        CommentViewRow comment = commentRepository.findViewById(commentId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Comment not found."));
        if (isDeleted(comment)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Cannot report a deleted comment.");
        }

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

    private void ensureRegisteredCommentUser(Long userId, UserRole role, String forbiddenMessage) {
        if (role == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Please log in to continue.");
        }
        if (userId == null || role == UserRole.GUEST) {
            throw new ApiException(HttpStatus.FORBIDDEN, forbiddenMessage);
        }
    }

    private CommentResponse toResponse(CommentViewRow row, Long viewerId, UserRole viewerRole, int replyCount) {
        boolean deleted = isDeleted(row);
        boolean edited = !deleted && row.updatedAt() != null;
        boolean owner = viewerId != null && viewerId.equals(row.userId());
        boolean admin = viewerRole == UserRole.ADMIN;
        boolean canEdit = owner && !deleted;
        boolean canDelete = (owner || admin) && !deleted;
        boolean canReply = !deleted
                && row.parentId() == null
                && viewerRole != null
                && viewerRole.canComment();

        return new CommentResponse(
                row.id(),
                row.parentId(),
                row.username(),
                UserRole.fromDatabaseValue(row.role()).label(),
                deleted ? "[Deleted]" : row.content(),
                formatDateTime(row.createdAt()),
                edited ? formatDateTime(row.updatedAt()) : null,
                edited,
                deleted,
                row.likes(),
                row.likedByMe() && !deleted,
                canEdit,
                canDelete,
                canReply,
                replyCount
        );
    }

    private CommentResponse toResponseWithLookup(CommentViewRow row, Long viewerId, UserRole viewerRole) {
        int replyCount = row.parentId() == null
                ? commentRepository.countActiveRepliesByParentId(row.id())
                : 0;
        return toResponse(row, viewerId, viewerRole, replyCount);
    }

    private boolean isDeleted(CommentViewRow row) {
        return "DELETED".equalsIgnoreCase(row.status());
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
