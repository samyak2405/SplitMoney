package com.splitmoney.document.storage;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

@Service
@ConditionalOnProperty(name = "storage.type", havingValue = "s3")
public class S3StorageService implements StorageService {

    private final S3Client s3;
    private final S3Presigner presigner;
    private final String bucket;

    public S3StorageService(
            @Value("${storage.s3.bucket}") String bucket,
            @Value("${storage.s3.region:us-east-1}") String region,
            @Value("${storage.s3.access-key:}") String accessKey,
            @Value("${storage.s3.secret-key:}") String secretKey
    ) {
        this.bucket = bucket;
        Region awsRegion = Region.of(region);

        StaticCredentialsProvider credentials = StaticCredentialsProvider.create(
                AwsBasicCredentials.create(accessKey, secretKey));

        this.s3 = S3Client.builder()
                .region(awsRegion)
                .credentialsProvider(credentials)
                .build();

        this.presigner = S3Presigner.builder()
                .region(awsRegion)
                .credentialsProvider(credentials)
                .build();
    }

    @Override
    public String store(MultipartFile file, String groupId, String documentId, String extension) {
        String key = "groups/" + groupId + "/" + documentId + "." + extension;
        try {
            PutObjectRequest req = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(file.getContentType())
                    .contentDisposition("inline; filename=\"" + file.getOriginalFilename() + "\"")
                    .build();
            s3.putObject(req, RequestBody.fromBytes(file.getBytes()));
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload to S3", e);
        }
        return key;
    }

    @Override
    public Resource load(String storagePath) {
        byte[] bytes = s3.getObjectAsBytes(
                GetObjectRequest.builder().bucket(bucket).key(storagePath).build()
        ).asByteArray();
        return new InputStreamResource(new ByteArrayInputStream(bytes));
    }

    @Override
    public void delete(String storagePath) {
        s3.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(storagePath).build());
    }

    @Override
    public boolean isRedirectBased() {
        return true;
    }

    @Override
    public Optional<String> presignedUrl(String storagePath, Duration ttl) {
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(ttl)
                .getObjectRequest(r -> r.bucket(bucket).key(storagePath))
                .build();
        PresignedGetObjectRequest presigned = presigner.presignGetObject(presignRequest);
        try {
            URI uri = presigned.url().toURI();
            return Optional.of(uri.toString());
        } catch (URISyntaxException e) {
            throw new RuntimeException("Failed to build presigned URL", e);
        }
    }
}
