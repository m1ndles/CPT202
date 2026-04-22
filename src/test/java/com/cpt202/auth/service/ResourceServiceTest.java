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
import com.cpt202.auth.repository.AdminArchiveRepository;
import com.cpt202.auth.repository.ResourceAppealMessageRepository;
import com.cpt202.auth.repository.ResourceRepository;
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
    private DraftAttachmentService draftAttachmentService;

    @Mock
    private SubmissionEmailService submissionEmailService;

    @InjectMocks
    private ResourceService resourceService;

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
}
