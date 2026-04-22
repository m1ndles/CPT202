package com.cpt202.auth.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cpt202.auth.dto.admin.AdminTaxonomyRequest;
import com.cpt202.auth.exception.ApiException;
import com.cpt202.auth.model.HeritageResource;
import com.cpt202.auth.repository.AdminActivityRepository;
import com.cpt202.auth.repository.AdminArchiveRepository;
import com.cpt202.auth.repository.AdminTaxonomyRepository;
import com.cpt202.auth.repository.AdminTaxonomyRepository.TaxonomyRecord;
import com.cpt202.auth.repository.ContributorApplicationRepository;
import com.cpt202.auth.repository.ResourceAppealMessageRepository;
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
 * Unit tests for {@link AdminConsoleService} covering review and taxonomy.
 */
@ExtendWith(MockitoExtension.class)
class AdminConsoleServiceTest {

    @Mock
    private ContributorApplicationRepository contributorApplicationRepository;

    @Mock
    private ContributorApplicationService contributorApplicationService;

    @Mock
    private ResourceRepository resourceRepository;

    @Mock
    private ResourceAppealMessageRepository resourceAppealMessageRepository;

    @Mock
    private AdminTaxonomyRepository adminTaxonomyRepository;

    @Mock
    private AdminArchiveRepository adminArchiveRepository;

    @Mock
    private AdminActivityRepository adminActivityRepository;

    @InjectMocks
    private AdminConsoleService adminConsoleService;

    /**
     * Creates a resource stub with the given id and status.
     */
    private HeritageResource resourceWithStatus(long id, String status) {
        return new HeritageResource(
                id, "Garden", "Garden", "Classical Garden", null, "Suzhou",
                "Desc", null, null, "TRK-" + id, status, 0,
                LocalDateTime.now(), 1L, "alice"
        );
    }

    /**
     * Approving a non-pending resource returns 400.
     */
    @Test
    void approveResourceReview_rejectsNonPending() {
        when(resourceRepository.findAnyById(50L))
                .thenReturn(Optional.of(resourceWithStatus(50L, "APPROVED")));

        assertThatThrownBy(() -> adminConsoleService.approveResourceReview(50L, "admin"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("pending")
                .extracting("status").isEqualTo(HttpStatus.BAD_REQUEST);

        verify(resourceRepository, never()).updateDraft(any());
    }

    /**
     * Rejection without comments returns 400.
     */
    @Test
    void rejectResourceReview_requiresComments() {
        when(resourceRepository.findAnyById(50L))
                .thenReturn(Optional.of(resourceWithStatus(50L, "PENDING")));

        assertThatThrownBy(() -> adminConsoleService.rejectResourceReview(50L, "   ", "admin"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Rejection comments")
                .extracting("status").isEqualTo(HttpStatus.BAD_REQUEST);

        verify(resourceRepository, never()).updateDraft(any());
    }

    /**
     * Creating a category with a duplicate name returns 409.
     */
    @Test
    void createCategory_rejectsDuplicateName() {
        when(adminTaxonomyRepository.countCategories()).thenReturn(1L);
        when(adminTaxonomyRepository.countTags()).thenReturn(1L);
        when(adminTaxonomyRepository.findCategoryByName("Classical Garden"))
                .thenReturn(Optional.of(new TaxonomyRecord(
                        1L, "Classical Garden", "existing", "ACTIVE", LocalDateTime.now())));

        assertThatThrownBy(() -> adminConsoleService.createCategory(
                new AdminTaxonomyRequest("Classical Garden", "A classical garden category."),
                "admin"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("already exists")
                .extracting("status").isEqualTo(HttpStatus.CONFLICT);

        verify(adminTaxonomyRepository, never()).insertCategory(
                anyString(), anyString(), anyString(), any(LocalDateTime.class));
    }

    /**
     * Toggling an active category switches it to inactive.
     */
    @Test
    void toggleCategoryStatus_togglesActiveToInactive() {
        TaxonomyRecord before = new TaxonomyRecord(
                1L, "Classical Garden", "desc", "ACTIVE", LocalDateTime.now());
        TaxonomyRecord after = new TaxonomyRecord(
                1L, "Classical Garden", "desc", "INACTIVE", LocalDateTime.now());

        when(adminTaxonomyRepository.countCategories()).thenReturn(1L);
        when(adminTaxonomyRepository.countTags()).thenReturn(1L);
        when(adminTaxonomyRepository.findCategoryById(1L)).thenReturn(Optional.of(before));
        when(resourceRepository.findAllResources()).thenReturn(List.of());
        when(adminTaxonomyRepository.findAllCategories()).thenReturn(List.of(after));

        adminConsoleService.toggleCategoryStatus(1L, "admin");

        verify(adminTaxonomyRepository).updateCategoryStatus(
                eq(1L), eq("INACTIVE"), any(LocalDateTime.class));
    }
}
