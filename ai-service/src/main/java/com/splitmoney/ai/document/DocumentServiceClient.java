package com.splitmoney.ai.document;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Slf4j
@Service
public class DocumentServiceClient {

    private final WebClient webClient;
    private final String baseUrl;

    public DocumentServiceClient(
            @Value("${ai.document-service.base-url:http://localhost:8084}") String baseUrl
    ) {
        this.baseUrl = baseUrl;
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                // Allow up to 8 MB in memory (covers the 5 MB AI limit + overhead)
                .codecs(c -> c.defaultCodecs().maxInMemorySize(8 * 1024 * 1024))
                .build();
    }

    /**
     * Fetches raw file bytes for a document from document-service.
     * Forwards the user's JWT so document-service accepts the request.
     *
     * @throws WebClientResponseException on HTTP 4xx/5xx
     * @throws RuntimeException on connection/timeout errors
     */
    public byte[] fetchDocumentBytes(String documentId, String userJwt) {
        log.info("Fetching document bytes documentId={} from {}", documentId, baseUrl);
        try {
            byte[] bytes = webClient.get()
                    .uri("/api/docs/v1/files/{id}", documentId)
                    .header("Authorization", "Bearer " + userJwt)
                    .retrieve()
                    .bodyToMono(byte[].class)
                    .block();
            log.info("Fetched document documentId={} size={} bytes",
                    documentId, bytes != null ? bytes.length : 0);
            return bytes;
        } catch (WebClientResponseException e) {
            log.error("Document fetch failed documentId={} status={} body={}",
                    documentId, e.getStatusCode(), e.getResponseBodyAsString());
            throw e;
        } catch (Exception e) {
            log.error("Document fetch error documentId={} error={}", documentId, e.getMessage());
            throw e;
        }
    }
}
