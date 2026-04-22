package com.cpt202.auth.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cpt202.auth.exception.ApiException;
import com.cpt202.auth.model.HeritageResource;
import com.cpt202.auth.repository.ResourceRepository;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;

/**
 * Unit tests for {@link DraftAttachmentService} covering upload validation.
 */
@ExtendWith(MockitoExtension.class)
class DraftAttachmentServiceTest {

    @Mock
    private ResourceRepository resourceRepository;

    private DraftAttachmentService draftAttachmentService;

    /**
     * Initialises the service with a temporary upload directory.
     */
    @BeforeEach
    void setUp(@TempDir Path tempDir) throws IOException {
        draftAttachmentService = new DraftAttachmentService(resourceRepository, tempDir.toString());
    }

    /**
     * Creates a resource stub with the given status.
     */
    private HeritageResource resource(String status) {
        return new HeritageResource(
                100L, "Title", "Title", "Cat", null, "Place",
                "Desc", null, null, "TRK-100", status, 0,
                LocalDateTime.now(), 1L, "alice"
        );
    }

    /**
     * Uploading to a non-draft resource returns 400.
     */
    @Test
    void uploadDraftAttachment_rejectsNonDraftStatus() {
        when(resourceRepository.findAnyById(100L)).thenReturn(Optional.of(resource("APPROVED")));

        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.jpg", "image/jpeg", "fake-bytes".getBytes());

        assertThatThrownBy(() -> draftAttachmentService.uploadDraftAttachment(100L, file))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("draft or rejected")
                .extracting("status").isEqualTo(HttpStatus.BAD_REQUEST);

        verify(resourceRepository, never()).insertAttachment(anyLong(), anyString(), anyString(), anyString());
    }

    /**
     * Uploading an unsupported file type returns 400.
     */
    @Test
    void uploadDraftAttachment_rejectsUnsupportedExtension() {
        when(resourceRepository.findAnyById(100L)).thenReturn(Optional.of(resource("DRAFT")));

        MockMultipartFile file = new MockMultipartFile(
                "file", "virus.exe", "application/octet-stream", "fake-bytes".getBytes());

        assertThatThrownBy(() -> draftAttachmentService.uploadDraftAttachment(100L, file))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Unsupported file type")
                .extracting("status").isEqualTo(HttpStatus.BAD_REQUEST);

        verify(resourceRepository, never()).insertAttachment(anyLong(), anyString(), anyString(), anyString());
    }
}
