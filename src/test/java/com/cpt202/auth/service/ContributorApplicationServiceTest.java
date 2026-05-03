package com.cpt202.auth.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cpt202.auth.dto.ContributorApplicationRequest;
import com.cpt202.auth.dto.ContributorApplicationResponse;
import com.cpt202.auth.exception.ApiException;
import com.cpt202.auth.model.UserAccount;
import com.cpt202.auth.model.UserRole;
import com.cpt202.auth.repository.AdminActivityRepository;
import com.cpt202.auth.repository.ContributorApplicationAppealMessageRepository;
import com.cpt202.auth.repository.ContributorApplicationRepository;
import com.cpt202.auth.repository.ResourceRepository;
import com.cpt202.auth.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

/**
 * Unit tests for {@link ContributorApplicationService} covering submission and review.
 */
@ExtendWith(MockitoExtension.class)
class ContributorApplicationServiceTest {

    @Mock
    private ContributorApplicationRepository contributorApplicationRepository;

    @Mock
    private ContributorApplicationAppealMessageRepository contributorApplicationAppealMessageRepository;

    @Mock
    private ResourceRepository resourceRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AdminActivityRepository adminActivityRepository;

    @InjectMocks
    private ContributorApplicationService contributorApplicationService;

    /**
     * Creates a test user with the given role.
     */
    private UserAccount user(UserRole role) {
        return new UserAccount(
                1L, "alice@example.com", "alice", "hash",
                role, 0, null, null, null, null, null
        );
    }

    /**
     * Creates a contributor application stub with the given status.
     */
    private ContributorApplicationResponse application(String status) {
        return new ContributorApplicationResponse(
                10L, 1L, "alice", "alice@example.com",
                "Alice Zhou", "Classical Garden", "I study gardens.",
                null, status, null,
                "2026-01-01 10:00", null,
                null, null,
                List.of(), "REJECTED".equals(status)
        );
    }

    /**
     * Duplicate submission while pending returns 409 Conflict.
     */
    @Test
    void submit_rejectsWhenPendingApplicationExists() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(UserRole.USER)));
        when(contributorApplicationRepository.findLatestByUserId(1L))
                .thenReturn(Optional.of(application("PENDING")));

        ContributorApplicationRequest request = new ContributorApplicationRequest(
                "Alice Zhou", "Classical Garden", "I want to contribute.", null);

        assertThatThrownBy(() -> contributorApplicationService.submit(1L, request, null))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("pending")
                .extracting("status").isEqualTo(HttpStatus.CONFLICT);

        verify(contributorApplicationRepository, never()).insert(
                anyLong(), anyString(), anyString(), anyString(), any(), any(), any());
    }

    /**
     * Approval upgrades the user role from USER to CONTRIBUTOR.
     */
    @Test
    void approve_upgradesUserRoleToContributor() {
        when(contributorApplicationRepository.findById(10L))
                .thenReturn(Optional.of(application("PENDING")));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(UserRole.USER)));

        contributorApplicationService.approve(10L, "admin");

        verify(contributorApplicationRepository).updateReview(eq(10L), eq("APPROVED"), any());
        verify(userRepository).updateUser(
                eq(1L),
                eq("alice@example.com"),
                eq("alice"),
                eq("hash"),
                eq(UserRole.CONTRIBUTOR));
    }

    /**
     * Rejection without comments returns 400 Bad Request.
     */
    @Test
    void reject_requiresRejectionComments() {
        assertThatThrownBy(() -> contributorApplicationService.reject(10L, "   ", "admin"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Rejection comments")
                .extracting("status").isEqualTo(HttpStatus.BAD_REQUEST);

        verify(contributorApplicationRepository, never()).updateReview(anyLong(), anyString(), anyString());
    }

    /**
     * Appeal submission requires a signed-in user session.
     */
    @Test
    void submitAppeal_rejectsWithoutSignedInUser() {
        assertThatThrownBy(() -> contributorApplicationService.submitAppeal(null, "Please reconsider."))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("log in")
                .extracting("status").isEqualTo(HttpStatus.UNAUTHORIZED);

        verify(contributorApplicationAppealMessageRepository, never())
                .insert(anyLong(), anyString(), anyString(), anyString(), any());
    }

    /**
     * Admin reply is rejected when the application has no active appeal thread.
     */
    @Test
    void replyToAppeal_rejectsWhenApplicationHasNoAppealThread() {
        when(contributorApplicationRepository.findById(10L))
                .thenReturn(Optional.of(application("PENDING")));
        when(contributorApplicationAppealMessageRepository.findByApplicationId(10L))
                .thenReturn(List.of());

        assertThatThrownBy(() -> contributorApplicationService.replyToAppeal(10L, "admin", "Please retry."))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("appeal thread")
                .extracting("status").isEqualTo(HttpStatus.BAD_REQUEST);

        verify(contributorApplicationAppealMessageRepository, never())
                .insert(anyLong(), anyString(), anyString(), anyString(), any());
    }
}
