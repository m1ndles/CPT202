package com.cpt202.auth.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cpt202.auth.exception.ApiException;
import com.cpt202.auth.model.HeritageResource;
import com.cpt202.auth.model.UserRole;
import com.cpt202.auth.repository.CommentRepository;
import com.cpt202.auth.repository.CommentRepository.CommentViewRow;
import com.cpt202.auth.repository.ResourceRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

/**
 * Unit tests for {@link CommentService} covering CRUD and nesting rules.
 */
@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private ResourceRepository resourceRepository;

    @InjectMocks
    private CommentService commentService;

    /**
     * Creates an approved resource stub for testing.
     */
    private HeritageResource approvedResource(long id) {
        return new HeritageResource(
                id, "Title", "Title", "Category", "Period", "Place",
                "Description", null, null, "TRK-1", "APPROVED", 0,
                LocalDateTime.now(), null, null
        );
    }

    /**
     * Creates a comment view row stub.
     */
    private CommentViewRow viewRow(long id, Long parentId, long userId, String status) {
        return new CommentViewRow(
                id,
                10L,
                userId,
                parentId,
                "alice",
                UserRole.USER.name(),
                "hello",
                status,
                LocalDateTime.now(),
                null,
                0,
                false
        );
    }

    /**
     * Fetching comments for a non-existent resource returns 404.
     */
    @Test
    void getComments_throwsWhenResourceMissing() {
        when(resourceRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.getComments(99L, 1L, UserRole.USER, 1, 10))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Resource not found")
                .extracting("status").isEqualTo(HttpStatus.NOT_FOUND);

        verify(commentRepository, never()).findRootsByResourceId(anyLong(), any(), anyInt(0), anyInt(0));
    }

    /**
     * Comment content is trimmed before persistence.
     */
    @Test
    void addComment_trimsWhitespaceAndSaves() {
        when(resourceRepository.findById(10L)).thenReturn(Optional.of(approvedResource(10L)));
        when(commentRepository.create(eq(10L), eq(1L), eq(null), eq("hello"))).thenReturn(55L);
        when(commentRepository.findViewById(55L, 1L)).thenReturn(Optional.of(viewRow(55L, null, 1L, "ACTIVE")));

        commentService.addComment(10L, 1L, UserRole.USER, "   hello   ");

        verify(commentRepository).create(10L, 1L, null, "hello");
    }

    /**
     * Guest users are forbidden from posting comments.
     */
    @Test
    void addComment_rejectsGuestRole() {
        assertThatThrownBy(() -> commentService.addComment(10L, 1L, UserRole.GUEST, "hi"))
                .isInstanceOf(ApiException.class)
                .extracting("status").isEqualTo(HttpStatus.FORBIDDEN);

        verify(commentRepository, never()).create(anyLong(), anyLong(), any(), anyString());
    }

    /**
     * Replies to replies are rejected (max 2 levels).
     */
    @Test
    void replyToComment_rejectsSecondLevelNesting() {
        CommentViewRow parentReply = viewRow(20L, 10L, 2L, "ACTIVE");
        when(commentRepository.findViewById(20L, 1L)).thenReturn(Optional.of(parentReply));

        assertThatThrownBy(() -> commentService.replyToComment(20L, 1L, UserRole.USER, "nested reply"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("2 levels")
                .extracting("status").isEqualTo(HttpStatus.BAD_REQUEST);

        verify(commentRepository, never()).create(anyLong(), anyLong(), any(), anyString());
    }

    /**
     * Editing another user's comment returns 403.
     */
    @Test
    void updateComment_rejectsWhenNotOwner() {
        CommentViewRow othersComment = viewRow(30L, null, 99L, "ACTIVE");
        when(commentRepository.findViewById(30L, 1L)).thenReturn(Optional.of(othersComment));

        assertThatThrownBy(() -> commentService.updateComment(30L, 1L, UserRole.USER, "edited"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("your own")
                .extracting("status").isEqualTo(HttpStatus.FORBIDDEN);

        verify(commentRepository, never()).updateContent(anyLong(), anyString());
    }

    /**
     * Deleting own comment performs a soft delete.
     */
    @Test
    void deleteComment_softDeletesOwnedComment() {
        CommentViewRow own = viewRow(40L, null, 1L, "ACTIVE");
        when(commentRepository.findViewById(40L, 1L)).thenReturn(Optional.of(own));

        commentService.deleteComment(40L, 1L, UserRole.USER);

        verify(commentRepository).softDelete(40L);
    }

    private static int anyInt(int defaultValue) {
        return org.mockito.ArgumentMatchers.anyInt();
    }
}
