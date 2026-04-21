package com.cpt202.auth.service;

import com.cpt202.auth.dto.CommentResponse;
import com.cpt202.auth.dto.PageResponse;
import com.cpt202.auth.exception.ApiException;
import com.cpt202.auth.model.UserRole;
import com.cpt202.auth.repository.CommentRepository;
import com.cpt202.auth.repository.CommentRepository.CommentViewRow;
import com.cpt202.auth.repository.ResourceRepository;
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

    /**
     * Default number of root comments per page.
     */
    private static final int DEFAULT_PAGE_SIZE = 10;

    /**
     * Maximum allowed number of root comments per page.
     */
    private static final int MAX_PAGE_SIZE = 50;

    /**
     * Formatter used for comment timestamps.
     */
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /**
     * Repository used to read and update comments.
     */
    private final CommentRepository commentRepository;

    /**
     * Repository used to validate target resources.
     */
    private final ResourceRepository resourceRepository;

    public CommentService(CommentRepository commentRepository, ResourceRepository resourceRepository) {
        this.commentRepository = commentRepository;
        this.resourceRepository = resourceRepository;
    }

    /**
     * Returns paged comments for a resource.
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

    /**
     * Creates a new root comment.
     */
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
     * Creates a reply under an existing root comment.
     */
    @Transactional
    public CommentResponse replyToComment(Long parentId, Long userId, UserRole role, String content) {
        ensureCommentPermission(userId, role);

        CommentViewRow parent = commentRepository.findViewById(parentId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Comment not found."));
        if (parent.parentId() != null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Replies cannot be nested beyond 2 levels.");
        }
        if ("DELETED".equalsIgnoreCase(parent.status())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Cannot reply to a deleted comment.");
        }

        long commentId = commentRepository.create(parent.resourceId(), userId, parentId, content.trim());
        CommentViewRow row = commentRepository.findViewById(commentId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Comment not found."));
        return toResponse(row, userId, role, 0);
    }

    /**
     * Updates an existing comment owned by the current user.
     */
    @Transactional
    public CommentResponse updateComment(Long commentId, Long userId, UserRole role, String content) {
        ensureCommentPermission(userId, role);

        CommentViewRow row = commentRepository.findViewById(commentId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Comment not found."));
        if (!row.userId().equals(userId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You can only edit your own comments.");
        }
        if ("DELETED".equalsIgnoreCase(row.status())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Deleted comments cannot be edited.");
        }

        commentRepository.updateContent(commentId, content.trim());
        CommentViewRow updated = commentRepository.findViewById(commentId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Comment not found."));
        return toResponseWithLookup(updated, userId, role);
    }

    /**
     * Soft-deletes a comment owned by the current user.
     */
    @Transactional
    public void deleteComment(Long commentId, Long userId, UserRole role) {
        ensureCommentPermission(userId, role);

        CommentViewRow row = commentRepository.findViewById(commentId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Comment not found."));
        if (!row.userId().equals(userId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You can only delete your own comments.");
        }
        if ("DELETED".equalsIgnoreCase(row.status())) {
            return;
        }

        commentRepository.softDelete(commentId);
    }

    /**
     * Toggles the current user's like on a comment.
     */
    @Transactional
    public CommentResponse toggleLike(Long commentId, Long userId, UserRole role) {
        ensureLikePermission(userId, role);

        CommentViewRow existing = commentRepository.findViewById(commentId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Comment not found."));
        if ("DELETED".equalsIgnoreCase(existing.status())) {
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

    /**
     * Ensures the target resource exists and is publicly visible.
     */
    private void ensureResourceExists(Long resourceId) {
        if (resourceRepository.findById(resourceId).isEmpty()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Resource not found.");
        }
    }

    /**
     * Ensures the current user can create or edit comments.
     */
    private void ensureCommentPermission(Long userId, UserRole role) {
        if (role == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Please log in to continue.");
        }
        if (userId == null || !role.canComment()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You do not have permission to comment.");
        }
    }

    /**
     * Ensures the current user can like comments.
     */
    private void ensureLikePermission(Long userId, UserRole role) {
        if (role == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Please log in to continue.");
        }
        if (userId == null || role == UserRole.GUEST) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Please use a registered account to like comments.");
        }
    }

    /**
     * Converts a repository view row into the API response shape.
     */
    private CommentResponse toResponse(CommentViewRow row, Long viewerId, UserRole viewerRole, int replyCount) {
        boolean deleted = "DELETED".equalsIgnoreCase(row.status());
        boolean edited = !deleted && row.updatedAt() != null;
        boolean owner = viewerId != null && viewerId.equals(row.userId());
        boolean canEdit = owner && !deleted;
        boolean canDelete = owner && !deleted;
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

    /**
     * Rebuilds a single comment response with a fresh reply count.
     */
    private CommentResponse toResponseWithLookup(CommentViewRow row, Long viewerId, UserRole viewerRole) {
        int replyCount = row.parentId() == null
                ? commentRepository.countActiveRepliesByParentId(row.id())
                : 0;
        return toResponse(row, viewerId, viewerRole, replyCount);
    }

    /**
     * Normalizes the requested page index.
     */
    private int normalizePage(int page) {
        return Math.max(page, 1);
    }

    /**
     * Normalizes the requested page size.
     */
    private int normalizeSize(int size) {
        if (size <= 0) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(size, MAX_PAGE_SIZE);
    }

    /**
     * Formats a comment timestamp for the API response.
     */
    private String formatDateTime(java.time.LocalDateTime value) {
        return value == null ? "" : DATE_TIME_FORMATTER.format(value);
    }
}
