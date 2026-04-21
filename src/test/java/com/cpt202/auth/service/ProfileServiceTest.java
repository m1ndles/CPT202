package com.cpt202.auth.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cpt202.auth.dto.UpdatePasswordRequest;
import com.cpt202.auth.exception.ApiException;
import com.cpt202.auth.model.UserAccount;
import com.cpt202.auth.model.UserRole;
import com.cpt202.auth.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuthService authService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private ProfileService profileService;

    private UserAccount user() {
        return new UserAccount(
                1L, "alice@example.com", "alice", "stored-hash",
                UserRole.USER, 0, null, null, null, null, null
        );
    }

    @Test
    void updatePassword_rejectsWrongCurrentPassword() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user()));
        when(passwordEncoder.matches("wrong-current", "stored-hash")).thenReturn(false);

        assertThatThrownBy(() -> profileService.updatePassword(
                1L, new UpdatePasswordRequest("wrong-current", "new-password")))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Current password")
                .extracting("status").isEqualTo(HttpStatus.BAD_REQUEST);

        verify(userRepository, never()).updatePasswordHash(anyLong(), anyString());
    }

    @Test
    void updatePassword_rejectsSameAsCurrent() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user()));
        when(passwordEncoder.matches("same-pw", "stored-hash")).thenReturn(true);

        assertThatThrownBy(() -> profileService.updatePassword(
                1L, new UpdatePasswordRequest("same-pw", "same-pw")))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("different")
                .extracting("status").isEqualTo(HttpStatus.BAD_REQUEST);

        verify(userRepository, never()).updatePasswordHash(anyLong(), anyString());
    }
}
