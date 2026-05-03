package com.splitmoney.document.storage;

import java.time.Duration;
import java.util.Optional;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface StorageService {

    String store(MultipartFile file, String groupId, String documentId, String extension);

    Resource load(String storagePath);

    void delete(String storagePath);

    boolean isRedirectBased();

    Optional<String> presignedUrl(String storagePath, Duration ttl);
}
