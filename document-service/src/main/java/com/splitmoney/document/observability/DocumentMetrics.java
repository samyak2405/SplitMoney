package com.splitmoney.document.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Component;

@Component
public class DocumentMetrics {

    private final MeterRegistry registry;

    public DocumentMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    public void recordUpload(String mimeType, String storageType, boolean success) {
        Counter.builder("document.upload")
                .tag("mime_type", mimeType)
                .tag("storage_type", storageType)  // local | s3
                .tag("success", String.valueOf(success))
                .description("Document uploads")
                .register(registry)
                .increment();
    }

    public void recordDownload(String storageType) {
        Counter.builder("document.download")
                .tag("storage_type", storageType)  // s3_redirect | local_stream
                .description("Document downloads served")
                .register(registry)
                .increment();
    }

    public void recordDelete(boolean success) {
        Counter.builder("document.delete")
                .tag("success", String.valueOf(success))
                .description("Document deletions")
                .register(registry)
                .increment();
    }

    public void recordS3OperationDuration(String operation, long durationMs) {
        Timer.builder("document.s3.operation")
                .tag("operation", operation)       // upload | presign | delete
                .description("S3 operation latency")
                .register(registry)
                .record(durationMs, TimeUnit.MILLISECONDS);
    }
}
