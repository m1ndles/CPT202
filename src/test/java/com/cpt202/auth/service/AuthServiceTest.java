package com.cpt202.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cpt202.auth.exception.AuthException;
import com.cpt202.auth.model.UserAccount;
import com.cpt202.auth.model.UserRole;
import com.cpt202.auth.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Unit tests for {@link AuthService} covering login and registration.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    /**
     * Creates a test user with the given failed-attempt state.
     */
    private UserAccount buildUser(int failedAttempts) {
        return new UserAccount(
                1L,
                "alice@example.com",
                "alice",
                "hash",
                UserRole.USER,
                failedAttempts,
                null,
                null,
                null,
                null,
                null
        );
    }

    /**
     * Successful login resets the failed-attempt counter.
     */
    @Test
    void login_success_resetsFailedCounter() {
        UserAccount user = buildUser(3);
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("correct-pw", "hash")).thenReturn(true);

        AuthService.AuthenticatedUser result = authService.login("Alice@Example.com", "correct-pw");

        assertThat(result.username()).isEqualTo("alice");
        assertThat(result.role()).isEqualTo(UserRole.USER);
        verify(userRepository).resetFailedLogin(1L);
    }

    /**
     * Sixth wrong password locks the account for 15 minutes.
     */
    @Test
    void login_wrongPassword_locksAtAttempt6() {
        UserAccount user = buildUser(5);
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-pw", "hash")).thenReturn(false);

        assertThatThrownBy(() -> authService.login("alice@example.com", "wrong-pw"))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining("locked")
                .extracting("status").isEqualTo(HttpStatus.LOCKED);

        verify(userRepository).updateFailedLogin(eq(1L), eq(6), any(LocalDateTime.class));
        verify(userRepository, never()).resetFailedLogin(1L);
    }

    /**
     * Registration with a taken username returns 409 Conflict.
     */
    @Test
    void register_duplicateUsername_throwsConflict() {
        when(userRepository.existsByUsername("alice")).thenReturn(true);

        assertThatThrownBy(() -> authService.register("alice@example.com", "alice", "pw"))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining("username")
                .extracting("status").isEqualTo(HttpStatus.CONFLICT);

        verify(userRepository, never()).createUser(any(), any(), any(), any());
    }

    /**
     * Registration with a taken email returns 409 Conflict.
     */
    @Test
    void register_duplicateEmail_throwsConflict() {
        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register("Alice@Example.com", "alice", "pw"))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining("email")
                .extracting("status").isEqualTo(HttpStatus.CONFLICT);

        verify(userRepository, never()).createUser(any(), any(), any(), any());
    }
}
