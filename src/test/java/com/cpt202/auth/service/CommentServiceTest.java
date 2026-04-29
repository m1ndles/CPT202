package com.cpt202.auth.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cpt202.auth.exception.ApiException;
import com.cpt202.auth.model.HeritageResource;
import com.cpt202.auth.model.UserRole;
import com.cpt202.auth.repository.CommentReportRepository;
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
 * Unit tests for {@link CommentService} covering the current comment and report workflow.
 */
@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private CommentReportRepository commentReportRepository;

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
    private CommentViewRow viewRow(long id, long userId, String viewerRole) {
        return new CommentViewRow(
                id,
                10L,
                userId,
                1L,
                "alice",
                UserRole.USER.name(),
                viewerRole,
                "hello",
                LocalDateTime.now(),
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

        assertThatThrownBy(() -> commentService.getComments(99L, 1L, 1, 10))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Resource not found")
                .extracting("status").isEqualTo(HttpStatus.NOT_FOUND);

        verify(commentRepository, never()).findByResourceId(anyLong(), anyLong(), anyInt(0), anyInt(0));
    }

    /**
     * Comment content is trimmed before persistence.
     */
    @Test
    void addComment_trimsWhitespaceAndSaves() {
        when(resourceRepository.findById(10L)).thenReturn(Optional.of(approvedResource(10L)));
        when(commentRepository.create(eq(10L), eq(1L), eq("hello"))).thenReturn(55L);
        when(commentRepository.findViewById(55L, 1L)).thenReturn(Optional.of(viewRow(55L, 1L, UserRole.USER.name())));

        commentService.addComment(10L, 1L, UserRole.USER, "   hello   ");

        verify(commentRepository).create(10L, 1L, "hello");
    }

    /**
     * Guest users are forbidden from posting comments.
     */
    @Test
    void addComment_rejectsGuestRole() {
        assertThatThrownBy(() -> commentService.addComment(10L, 1L, UserRole.GUEST, "hi"))
                .isInstanceOf(ApiException.class)
                .extracting("status").isEqualTo(HttpStatus.FORBIDDEN);

        verify(commentRepository, never()).create(anyLong(), anyLong(), anyString());
    }

    /**
     * Regular users cannot delete comments in the latest moderation model.
     */
    @Test
    void deleteComment_rejectsRegularUserDeletion() {
        when(commentRepository.findViewById(40L, 1L))
                .thenReturn(Optional.of(viewRow(40L, 1L, UserRole.USER.name())));

        assertThatThrownBy(() -> commentService.deleteComment(40L, 1L, UserRole.USER))
                .isInstanceOf(ApiException.class)
                .extracting("status").isEqualTo(HttpStatus.FORBIDDEN);

        verify(commentRepository, never()).delete(anyLong());
    }

    /**
     * Contributors can delete their own comments.
     */
    @Test
    void deleteComment_deletesContributorOwnComment() {
        when(commentRepository.findViewById(40L, 1L))
                .thenReturn(Optional.of(viewRow(40L, 1L, UserRole.CONTRIBUTOR.name())));

        commentService.deleteComment(40L, 1L, UserRole.CONTRIBUTOR);

        verify(commentRepository).delete(40L);
    }

    /**
     * Empty report content is rejected before a report thread is created.
     */
    @Test
    void submitReport_requiresReportContent() {
        when(commentRepository.findViewById(40L, 1L))
                .thenReturn(Optional.of(viewRow(40L, 1L, UserRole.USER.name())));

        assertThatThrownBy(() -> commentService.submitReport(40L, 1L, UserRole.USER, "alice", "   "))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Report content")
                .extracting("status").isEqualTo(HttpStatus.BAD_REQUEST);

        verify(commentReportRepository, never()).createThread(anyLong(), anyLong(), anyString(), anyString(), org.mockito.ArgumentMatchers.any());
    }

    private static int anyInt(int defaultValue) {
        return org.mockito.ArgumentMatchers.anyInt();
    }
}
