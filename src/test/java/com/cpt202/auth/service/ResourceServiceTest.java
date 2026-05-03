package com.cpt202.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cpt202.auth.dto.PageResponse;
import com.cpt202.auth.dto.ResourceSummary;
import com.cpt202.auth.exception.ApiException;
import com.cpt202.auth.model.HeritageResource;
import com.cpt202.auth.repository.AdminArchiveRepository;
import com.cpt202.auth.repository.ResourceAppealMessageRepository;
import com.cpt202.auth.repository.ResourceRepository;
import com.cpt202.auth.repository.ResourceReportRepository;
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
 * Unit tests for {@link ResourceService} covering browsing and favourites.
 */
@ExtendWith(MockitoExtension.class)
class ResourceServiceTest {

    @Mock
    private ResourceRepository resourceRepository;

    @Mock
    private AdminArchiveRepository adminArchiveRepository;

    @Mock
    private ResourceAppealMessageRepository resourceAppealMessageRepository;

    @Mock
    private ResourceReportRepository resourceReportRepository;

    @Mock
    private DraftAttachmentService draftAttachmentService;

    @Mock
    private SubmissionEmailService submissionEmailService;

    @InjectMocks
    private ResourceService resourceService;

    /**
     * Builds a stub heritage resource with the given id and status.
     */
    private HeritageResource resourceWithStatus(long id, String status) {
        return new HeritageResource(
                id, "Garden", "Garden", "Classical Garden", null, "Suzhou",
                "Desc", null, null, "TRK-" + id, status, 0,
                LocalDateTime.now(), 1L, "alice"
        );
    }

    /**
     * Empty database yields an empty page response.
     */
    @Test
    void getResources_returnsEmptyPageWhenNoData() {
        when(resourceRepository.countApproved(null, null, null)).thenReturn(0L);
        when(resourceRepository.findApproved(any(), any(), any(), anyString(), anyInt(), anyInt()))
                .thenReturn(List.of());

        PageResponse<ResourceSummary> page = resourceService.getResources(null, null, null, null, 1, 6);

        assertThat(page.totalItems()).isZero();
        assertThat(page.totalPages()).isZero();
        assertThat(page.content()).isEmpty();
    }

    /**
     * Requesting a non-existent resource returns 404.
     */
    @Test
    void getResource_throwsNotFoundForMissingResource() {
        when(resourceRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> resourceService.getResource(99L, null))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("not found")
                .extracting("status").isEqualTo(HttpStatus.NOT_FOUND);
    }

    /**
     * View count increment on missing resource returns 404.
     */
    @Test
    void incrementView_throwsNotFoundForMissingResource() {
        when(resourceRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> resourceService.incrementView(99L))
                .isInstanceOf(ApiException.class)
                .extracting("status").isEqualTo(HttpStatus.NOT_FOUND);

        verify(resourceRepository, never()).incrementViewCount(anyLong());
    }

    /**
     * Favourite toggle on missing resource returns 404.
     */
    @Test
    void toggleFavorite_throwsNotFoundForMissingResource() {
        when(resourceRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> resourceService.toggleFavorite(99L, 1L))
                .isInstanceOf(ApiException.class)
                .extracting("status").isEqualTo(HttpStatus.NOT_FOUND);

        verify(resourceRepository, never()).addFavorite(anyLong(), anyLong());
        verify(resourceRepository, never()).removeFavorite(anyLong(), anyLong());
    }

    /**
     * Whitespace-only report content is rejected before any thread is opened.
     */
    @Test
    void submitReport_requiresReportContent() {
        when(resourceRepository.findById(10L)).thenReturn(Optional.of(resourceWithStatus(10L, "APPROVED")));

        assertThatThrownBy(() -> resourceService.submitReport(10L, 1L, "alice", "   "))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Report content")
                .extracting("status").isEqualTo(HttpStatus.BAD_REQUEST);

        verify(resourceReportRepository, never())
                .createThread(anyLong(), anyLong(), anyString(), anyString(), any());
        verify(resourceReportRepository, never())
                .insertMessage(anyLong(), anyString(), anyString(), anyString(), any());
    }

    /**
     * Appeal submission requires a signed-in contributor session.
     */
    @Test
    void submitAppeal_rejectsWithoutSignedInUser() {
        assertThatThrownBy(() -> resourceService.submitAppeal(10L, null, "alice", "Please reconsider."))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("log in")
                .extracting("status").isEqualTo(HttpStatus.UNAUTHORIZED);

        verify(resourceAppealMessageRepository, never())
                .insert(anyLong(), anyString(), anyString(), anyString(), any());
    }
}
