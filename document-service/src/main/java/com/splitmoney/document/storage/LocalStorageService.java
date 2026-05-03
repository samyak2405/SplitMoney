package com.splitmoney.document.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@ConditionalOnProperty(name = "storage.type", havingValue = "local", matchIfMissing = true)
public class LocalStorageService implements StorageService {

    private final Path basePath;

    public LocalStorageService(@Value("${storage.local.base-path:./uploads}") String basePath) {
        this.basePath = Paths.get(basePath).toAbsolutePath();
    }

    @Override
    public String store(MultipartFile file, String groupId, String documentId, String extension) {
        try {
            Path dir = basePath.resolve(groupId);
            Files.createDirectories(dir);
            String fileName = documentId + "." + extension;
            Path dest = dir.resolve(fileName);
            file.transferTo(dest);
            return groupId + "/" + fileName;
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file", e);
        }
    }

    @Override
    public Resource load(String storagePath) {
        return new FileSystemResource(basePath.resolve(storagePath));
    }

    @Override
    public void delete(String storagePath) {
        try {
            Files.deleteIfExists(basePath.resolve(storagePath));
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete file", e);
        }
    }

    @Override
    public boolean isRedirectBased() {
        return false;
    }

    @Override
    public Optional<String> presignedUrl(String storagePath, Duration ttl) {
        return Optional.empty();
    }
}
