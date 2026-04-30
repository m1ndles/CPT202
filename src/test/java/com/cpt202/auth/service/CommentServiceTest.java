package com.cpt202.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cpt202.auth.dto.PageResponse;
import com.cpt202.auth.exception.ApiException;
import com.cpt202.auth.model.HeritageResource;
import com.cpt202.auth.model.UserRole;
import com.cpt202.auth.repository.CommentReportRepository;
import com.cpt202.auth.repository.CommentRepository;
import com.cpt202.auth.repository.CommentRepository.CommentViewRow;
import com.cpt202.auth.repository.ResourceRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

/**
 * Unit tests for {@link CommentService} covering threaded comments and reports.
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

    private HeritageResource approvedResource(long id) {
        return new HeritageResource(
                id, "Title", "Title", "Category", "Period", "Place",
                "Description", null, null, "TRK-1", "APPROVED", 0,
                LocalDateTime.now(), null, null
        );
    }

    private CommentViewRow viewRow(long id, long userId, Long parentId, String status) {
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

    @Test
    void getComments_throwsWhenResourceMissing() {
        when(resourceRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.getComments(99L, 1L, UserRole.USER, 1, 10))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Resource not found")
                .extracting("status").isEqualTo(HttpStatus.NOT_FOUND);

        verify(commentRepository, never()).findRootsByResourceId(anyLong(), anyLong(), anyInt(), anyInt());
    }

    @Test
    void getComments_flattensRootsAndRepliesWithReplyPermissions() {
        CommentViewRow root = viewRow(40L, 2L, null, "ACTIVE");
        CommentViewRow reply = viewRow(41L, 3L, 40L, "ACTIVE");
        when(resourceRepository.findById(10L)).thenReturn(Optional.of(approvedResource(10L)));
        when(commentRepository.countRootsByResourceId(10L)).thenReturn(1L);
        when(commentRepository.findRootsByResourceId(10L, 1L, 0, 10)).thenReturn(List.of(root));
        when(commentRepository.findRepliesByParentIds(List.of(40L), 1L)).thenReturn(List.of(reply));

        PageResponse<?> response = commentService.getComments(10L, 1L, UserRole.USER, 1, 10);

        assertThat(response.content()).hasSize(2);
        assertThat(response.content().get(0))
                .extracting("id", "canReply", "replyCount")
                .containsExactly(40L, true, 1);
        assertThat(response.content().get(1))
                .extracting("id", "parentId", "canReply")
                .containsExactly(41L, 40L, false);
    }

    @Test
    void addComment_trimsWhitespaceAndSavesRootComment() {
        when(resourceRepository.findById(10L)).thenReturn(Optional.of(approvedResource(10L)));
        when(commentRepository.create(eq(10L), eq(1L), eq(null), eq("hello"))).thenReturn(55L);
        when(commentRepository.findViewById(55L, 1L)).thenReturn(Optional.of(viewRow(55L, 1L, null, "ACTIVE")));

        commentService.addComment(10L, 1L, UserRole.USER, "   hello   ");

        verify(commentRepository).create(10L, 1L, null, "hello");
    }

    @Test
    void addComment_rejectsGuestRole() {
        assertThatThrownBy(() -> commentService.addComment(10L, 1L, UserRole.GUEST, "hi"))
                .isInstanceOf(ApiException.class)
                .extracting("status").isEqualTo(HttpStatus.FORBIDDEN);

        verify(commentRepository, never()).create(anyLong(), anyLong(), any(), anyString());
    }

    @Test
    void replyToComment_rejectsNestedReply() {
        when(commentRepository.findViewById(41L, 1L))
                .thenReturn(Optional.of(viewRow(41L, 3L, 40L, "ACTIVE")));

        assertThatThrownBy(() -> commentService.replyToComment(41L, 1L, UserRole.USER, "nested"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("2 levels")
                .extracting("status").isEqualTo(HttpStatus.BAD_REQUEST);

        verify(commentRepository, never()).create(anyLong(), anyLong(), anyLong(), anyString());
    }

    @Test
    void updateComment_updatesOwnActiveComment() {
        when(commentRepository.findViewById(40L, 1L))
                .thenReturn(Optional.of(viewRow(40L, 1L, null, "ACTIVE")))
                .thenReturn(Optional.of(new CommentViewRow(
                        40L,
                        10L,
                        1L,
                        null,
                        "alice",
                        UserRole.USER.name(),
                        "updated",
                        "ACTIVE",
                        LocalDateTime.now().minusMinutes(5),
                        LocalDateTime.now(),
                        0,
                        false
                )));

        commentService.updateComment(40L, 1L, UserRole.USER, " updated ");

        verify(commentRepository).updateContent(40L, "updated");
    }

    @Test
    void deleteComment_softDeletesOwnComment() {
        when(commentRepository.findViewById(40L, 1L))
                .thenReturn(Optional.of(viewRow(40L, 1L, null, "ACTIVE")));

        commentService.deleteComment(40L, 1L, UserRole.USER);

        verify(commentRepository).softDelete(40L);
    }

    @Test
    void deleteComment_rejectsNonOwner() {
        when(commentRepository.findViewById(40L, 1L))
                .thenReturn(Optional.of(viewRow(40L, 2L, null, "ACTIVE")));

        assertThatThrownBy(() -> commentService.deleteComment(40L, 1L, UserRole.USER))
                .isInstanceOf(ApiException.class)
                .extracting("status").isEqualTo(HttpStatus.FORBIDDEN);

        verify(commentRepository, never()).softDelete(anyLong());
    }

    @Test
    void submitReport_requiresReportContent() {
        when(commentRepository.findViewById(40L, 1L))
                .thenReturn(Optional.of(viewRow(40L, 1L, null, "ACTIVE")));

        assertThatThrownBy(() -> commentService.submitReport(40L, 1L, UserRole.USER, "alice", "   "))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Report content")
                .extracting("status").isEqualTo(HttpStatus.BAD_REQUEST);

        verify(commentReportRepository, never()).createThread(anyLong(), anyLong(), anyString(), anyString(), any());
    }
}
