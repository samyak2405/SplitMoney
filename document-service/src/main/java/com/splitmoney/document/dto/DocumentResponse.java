package com.splitmoney.document.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DocumentResponse {
    private String documentId;
    private String fileName;
    private long fileSize;
    private String mimeType;
    private String url;
    private String uploaderEmail;
    private long createdAt;
}
