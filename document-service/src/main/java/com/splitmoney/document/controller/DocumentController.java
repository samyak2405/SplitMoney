package com.splitmoney.document.controller;

import com.splitmoney.document.domain.Document;
import com.splitmoney.document.dto.ApiBody;
import com.splitmoney.document.dto.DocumentResponse;
import com.splitmoney.document.repository.DocumentRepository;
import com.splitmoney.document.security.JwtAuthenticationFilter;
import com.splitmoney.document.storage.StorageService;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@RestController
@RequestMapping("/api/docs/v1")
public class DocumentController {

    private static final List<String> ALLOWED_MIME_TYPES = List.of(
            "image/jpeg", "image/png", "image/gif", "image/webp", "application/pdf"
    );

    // Magic bytes: first bytes of each allowed type
    private static final byte[] JPEG_MAGIC = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] PNG_MAGIC  = {(byte) 0x89, 0x50, 0x4E, 0x47};
    private static final byte[] GIF_MAGIC  = {0x47, 0x49, 0x46};
    private static final byte[] WEBP_MAGIC = {0x52, 0x49, 0x46, 0x46};
    private static final byte[] PDF_MAGIC  = {0x25, 0x50, 0x44, 0x46};

    private final DocumentRepository documentRepository;
    private final StorageService storageService;
    private final long maxFileSizeMb;

    public DocumentController(
            DocumentRepository documentRepository,
            StorageService storageService,
            @Value("${storage.max-file-size-mb:10}") long maxFileSizeMb
    ) {
        this.documentRepository = documentRepository;
        this.storageService = storageService;
        this.maxFileSizeMb = maxFileSizeMb;
    }

    // ── Upload ─────────────────────────────────────────────────────────────

    @PostMapping("/groups/{groupId}/upload")
    public ResponseEntity<ApiBody<DocumentResponse>> upload(
            @PathVariable String groupId,
            @RequestParam("file") MultipartFile file,
            HttpServletRequest request
    ) throws IOException {
        UUID userId = (UUID) request.getAttribute(JwtAuthenticationFilter.AUTHENTICATED_USER_ID_ATTR);
        String email = (String) request.getAttribute(JwtAuthenticationFilter.AUTHENTICATED_USER_EMAIL_ATTR);

        // Size check
        long maxBytes = maxFileSizeMb * 1024 * 1024;
        if (file.getSize() > maxBytes) {
            return badRequest("File exceeds maximum size of " + maxFileSizeMb + " MB");
        }

        // MIME type + magic bytes validation
        String mimeType = file.getContentType();
        if (mimeType == null || !ALLOWED_MIME_TYPES.contains(mimeType)) {
            return badRequest("File type not allowed. Supported: JPEG, PNG, GIF, WEBP, PDF");
        }

        byte[] header = file.getBytes().length >= 4 ? Arrays.copyOf(file.getBytes(), 4) : file.getBytes();
        if (!isValidMagic(mimeType, header)) {
            return badRequest("File content does not match the declared type");
        }

        // Determine extension
        String ext = extensionFor(mimeType);
        String documentId = UUID.randomUUID().toString();
        String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : documentId + "." + ext;

        // Store
        String storagePath = storageService.store(file, groupId, documentId, ext);

        // Persist metadata
        Document doc = new Document();
        doc.setGroupId(groupId);
        doc.setUploaderId(userId.toString());
        doc.setUploaderEmail(email);
        doc.setFileName(documentId + "." + ext);
        doc.setOriginalName(originalName);
        doc.setMimeType(mimeType);
        doc.setFileSize(file.getSize());
        doc.setStoragePath(storagePath);
        documentRepository.save(doc);

        DocumentResponse resp = toResponse(doc);
        return ResponseEntity.ok(ApiBody.<DocumentResponse>builder().success(true).data(resp).build());
    }

    // ── List documents for a group ─────────────────────────────────────────

    @GetMapping("/groups/{groupId}/documents")
    public ResponseEntity<ApiBody<List<DocumentResponse>>> listDocuments(
            @PathVariable String groupId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        List<DocumentResponse> docs = documentRepository
                .findByGroupIdOrderByCreatedAtDesc(groupId, PageRequest.of(page, Math.min(size, 50)))
                .stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(ApiBody.<List<DocumentResponse>>builder().success(true).data(docs).build());
    }

    // ── Serve file ─────────────────────────────────────────────────────────

    @GetMapping("/files/{documentId}")
    public ResponseEntity<?> serveFile(@PathVariable String documentId) {
        UUID id;
        try {
            id = UUID.fromString(documentId);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }

        Optional<Document> optional = documentRepository.findById(id);
        if (optional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Document doc = optional.get();

        if (storageService.isRedirectBased()) {
            String url = storageService.presignedUrl(doc.getStoragePath(), Duration.ofHours(1))
                    .orElseThrow(() -> new RuntimeException("Presigned URL generation failed"));
            return ResponseEntity.status(HttpStatus.FOUND)
                    .header(HttpHeaders.LOCATION, url)
                    .build();
        }

        Resource resource = storageService.load(doc.getStoragePath());
        StreamingResponseBody body = out -> {
            try (var in = resource.getInputStream()) {
                in.transferTo(out);
            }
        };

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(doc.getMimeType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + doc.getOriginalName() + "\"")
                .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(doc.getFileSize()))
                .body(body);
    }

    // ── Delete ─────────────────────────────────────────────────────────────

    @DeleteMapping("/documents/{documentId}")
    public ResponseEntity<ApiBody<Void>> deleteDocument(
            @PathVariable String documentId,
            HttpServletRequest request
    ) {
        UUID userId = (UUID) request.getAttribute(JwtAuthenticationFilter.AUTHENTICATED_USER_ID_ATTR);

        UUID id;
        try {
            id = UUID.fromString(documentId);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }

        Optional<Document> optional = documentRepository.findById(id);
        if (optional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Document doc = optional.get();

        if (!doc.getUploaderId().equals(userId.toString())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiBody.<Void>builder().success(false).responseMessage("You can only delete your own files").build());
        }

        storageService.delete(doc.getStoragePath());
        documentRepository.delete(doc);
        return ResponseEntity.ok(ApiBody.<Void>builder().success(true).build());
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private DocumentResponse toResponse(Document doc) {
        return DocumentResponse.builder()
                .documentId(doc.getId().toString())
                .fileName(doc.getOriginalName())
                .fileSize(doc.getFileSize())
                .mimeType(doc.getMimeType())
                .url("/api/docs/v1/files/" + doc.getId())
                .uploaderEmail(doc.getUploaderEmail())
                .createdAt(doc.getCreatedAt().toEpochMilli())
                .build();
    }

    private boolean isValidMagic(String mimeType, byte[] header) {
        return switch (mimeType) {
            case "image/jpeg" -> startsWith(header, JPEG_MAGIC);
            case "image/png"  -> startsWith(header, PNG_MAGIC);
            case "image/gif"  -> startsWith(header, GIF_MAGIC);
            case "image/webp" -> startsWith(header, WEBP_MAGIC);
            case "application/pdf" -> startsWith(header, PDF_MAGIC);
            default -> false;
        };
    }

    private boolean startsWith(byte[] data, byte[] prefix) {
        if (data.length < prefix.length) return false;
        for (int i = 0; i < prefix.length; i++) {
            if (data[i] != prefix[i]) return false;
        }
        return true;
    }

    private String extensionFor(String mimeType) {
        return switch (mimeType) {
            case "image/jpeg" -> "jpg";
            case "image/png"  -> "png";
            case "image/gif"  -> "gif";
            case "image/webp" -> "webp";
            case "application/pdf" -> "pdf";
            default -> "bin";
        };
    }

    private <T> ResponseEntity<ApiBody<T>> badRequest(String message) {
        return ResponseEntity.badRequest()
                .body(ApiBody.<T>builder().success(false).responseMessage(message).build());
    }
}
