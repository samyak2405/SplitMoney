package com.splitmoney.document.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "documents", schema = "documentdb")
@Getter
@Setter
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "group_id", nullable = false)
    private String groupId;

    @Column(name = "uploader_id", nullable = false)
    private String uploaderId;

    @Column(name = "uploader_email")
    private String uploaderEmail;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "original_name", nullable = false)
    private String originalName;

    @Column(name = "mime_type", nullable = false)
    private String mimeType;

    @Column(name = "file_size", nullable = false)
    private long fileSize;

    @Column(name = "storage_path", nullable = false)
    private String storagePath;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
}
