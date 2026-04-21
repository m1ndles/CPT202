package com.cpt202.auth.service;

import com.cpt202.auth.dto.DraftAttachmentResponse;
import com.cpt202.auth.exception.ApiException;
import com.cpt202.auth.model.HeritageResource;
import com.cpt202.auth.repository.ResourceRepository;
import java.io.IOException;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

/**
 * Draft attachment storage and linkage.
 */
@Service
public class DraftAttachmentService {

    /**
     * Maximum upload size for a draft attachment.
     */
    private static final long MAX_FILE_SIZE = 10L * 1024L * 1024L;

    /**
     * Supported draft attachment extensions.
     */
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "pdf", "mp3", "wav");

    /**
     * Extensions treated as image attachments.
     */
    private static final Set<String> IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png");

    /**
     * Resource statuses that may expose draft-style attachments.
     */
    private static final Set<String> VIEWABLE_STATUSES = Set.of("DRAFT", "PENDING", "REJECTED");

    /**
     * Repository used to link stored files with resources.
     */
    private final ResourceRepository resourceRepository;

    /**
     * Absolute upload directory for stored attachment files.
     */
    private final Path uploadDir;

    public DraftAttachmentService(
            ResourceRepository resourceRepository,
            @Value("${app.upload-dir:uploads}") String uploadDir
    ) throws IOException {
        this.resourceRepository = resourceRepository;
        this.uploadDir = Path.of(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(this.uploadDir);
    }

    /**
     * Stores an uploaded file and links it to the target draft.
     */
    public DraftAttachmentResponse uploadDraftAttachment(Long draftId, MultipartFile file) {
        HeritageResource draft = requireEditableDraft(draftId);

        if (file == null || file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Please select a file to upload.");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Files larger than 10MB are not allowed.");
        }

        String originalName = StringUtils.cleanPath(file.getOriginalFilename() == null ? "file" : file.getOriginalFilename());
        String extension = getExtension(originalName);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Unsupported file type. Only JPG, PNG, PDF, MP3, and WAV are allowed.");
        }

        String storedName = draft.id() + "-" + UUID.randomUUID() + "." + extension;
        Path target = uploadDir.resolve(storedName).normalize();
        if (!target.startsWith(uploadDir)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid upload path.");
        }

        try {
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to store attachment.");
        }

        String fileType = resolveFileType(extension);
        String accessUrl = "/api/resources/files/" + storedName;
        Long attachmentId = resourceRepository.insertAttachment(draft.id(), originalName, fileType, accessUrl);
        return new DraftAttachmentResponse(attachmentId, originalName, fileType, accessUrl);
    }

    /**
     * Deletes a stored draft attachment and its database record.
     */
    public void removeDraftAttachment(Long draftId, Long attachmentId) {
        HeritageResource draft = requireEditableDraft(draftId);

        ResourceRepository.AttachmentRecord attachment = resourceRepository.findAttachmentById(draft.id(), attachmentId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Attachment not found."));

        deleteStoredFile(attachment.url());
        resourceRepository.deleteAttachment(draft.id(), attachmentId);
    }

    /**
     * Loads a stored attachment as a Spring resource.
     */
    public Resource loadAttachment(String storedName) {
        Path file = uploadDir.resolve(storedName).normalize();
        if (!file.startsWith(uploadDir) || !Files.exists(file)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Attachment not found.");
        }
        return new FileSystemResource(file);
    }

    /**
     * Detects the content type for a stored attachment.
     */
    public String detectContentType(String storedName) {
        try {
            String probe = Files.probeContentType(uploadDir.resolve(storedName));
            return probe == null ? URLConnection.guessContentTypeFromName(storedName) : probe;
        } catch (IOException ex) {
            return "application/octet-stream";
        }
    }

    /**
     * Returns all attachments linked to a draft-like resource.
     */
    public List<DraftAttachmentResponse> getDraftAttachments(Long resourceId) {
        requireViewableDraftLikeResource(resourceId);
        return resourceRepository.findDraftAttachments(resourceId).stream()
                .map(row -> new DraftAttachmentResponse(row.id(), row.name(), row.type(), row.url()))
                .toList();
    }

    /**
     * Removes every stored file attached to the given resource.
     */
    public void removeStoredFilesForResource(Long resourceId) {
        resourceRepository.findDraftAttachments(resourceId)
                .forEach(attachment -> deleteStoredFile(attachment.url()));
    }

    /**
     * Ensures the target resource is editable as a draft.
     */
    private HeritageResource requireEditableDraft(Long draftId) {
        HeritageResource resource = resourceRepository.findAnyById(draftId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Draft not found."));
        String normalized = String.valueOf(resource.status()).toUpperCase(Locale.ROOT);
        if (!"DRAFT".equals(normalized) && !"REJECTED".equals(normalized)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Only draft or rejected resources can be edited.");
        }
        return resource;
    }

    /**
     * Ensures the target resource can expose draft attachments.
     */
    private void requireViewableDraftLikeResource(Long resourceId) {
        HeritageResource resource = resourceRepository.findAnyById(resourceId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Draft not found."));
        if (!VIEWABLE_STATUSES.contains(resource.status().toUpperCase(Locale.ROOT))) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Draft not found.");
        }
    }

    /**
     * Removes a stored attachment file from disk.
     */
    private void deleteStoredFile(String url) {
        String prefix = "/api/resources/files/";
        if (url == null || !url.startsWith(prefix)) {
            return;
        }
        String storedName = url.substring(prefix.length());
        try {
            Files.deleteIfExists(uploadDir.resolve(storedName));
        } catch (IOException ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to remove attachment file.");
        }
    }

    /**
     * Returns the lowercase extension for a file name.
     */
    private String getExtension(String fileName) {
        int index = fileName.lastIndexOf('.');
        if (index < 0 || index == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(index + 1).toLowerCase(Locale.ROOT);
    }

    /**
     * Maps an extension to the attachment type used by the API.
     */
    private String resolveFileType(String extension) {
        if (IMAGE_EXTENSIONS.contains(extension)) {
            return "image";
        }
        if ("pdf".equals(extension)) {
            return "document";
        }
        return "audio";
    }
}
