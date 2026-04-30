package com.splitmoney.application.domain.idempotency;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.splitmoney.application.config.IdempotencyProperties;
import com.splitmoney.application.domain.patterns.exception.IdempotencyConflictException;
import com.splitmoney.application.infra.persistence.IdempotencyRecordRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IdempotencyService {

    private static final Logger LOGGER = LoggerFactory.getLogger(IdempotencyService.class);
    private final StringRedisTemplate redisTemplate;
    private final IdempotencyRecordRepository repository;
    private final IdempotencyProperties properties;
    private final ObjectMapper objectMapper;

    public IdempotencyService(
            StringRedisTemplate redisTemplate,
            IdempotencyRecordRepository repository,
            IdempotencyProperties properties,
            ObjectMapper objectMapper
    ) {
        this.redisTemplate = redisTemplate;
        this.repository = repository;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public <T> Optional<T> findExistingResponse(
            String endpoint,
            String idempotencyKey,
            Object request,
            Class<T> responseType
    ) {
        String requestHash = hashRequest(request);
        Optional<IdempotencyRecord> existing = repository.findByIdempotencyKeyAndEndpoint(idempotencyKey, endpoint);
        if (existing.isEmpty()) {
            return Optional.empty();
        }

        IdempotencyRecord record = existing.get();
        if (!record.getRequestHash().equals(requestHash)) {
            LOGGER.warn("idempotency conflict endpoint={} idempotencyKey={}", endpoint, idempotencyKey);
            throw new IdempotencyConflictException("Idempotency key reused with different payload");
        }

        try {
            LOGGER.info("idempotent response replayed endpoint={} idempotencyKey={} paymentId={}",
                    endpoint, idempotencyKey, record.getPaymentId());
            return Optional.of(objectMapper.readValue(record.getResponseBody(), responseType));
        } catch (JsonProcessingException ex) {
            throw new IdempotencyConflictException("Stored idempotency response cannot be parsed");
        }
    }

    @Transactional
    public void saveResponse(
            String endpoint,
            String idempotencyKey,
            Object request,
            UUID paymentId,
            int statusCode,
            Object response
    ) {
        IdempotencyRecord record = new IdempotencyRecord();
        record.setId(UUID.randomUUID());
        record.setIdempotencyKey(idempotencyKey);
        record.setEndpoint(endpoint);
        record.setRequestHash(hashRequest(request));
        record.setPaymentId(paymentId);
        record.setResponseStatus(statusCode);
        record.setCreatedAt(OffsetDateTime.now());
        try {
            record.setResponseBody(objectMapper.writeValueAsString(response));
        } catch (JsonProcessingException ex) {
            throw new IdempotencyConflictException("Unable to serialize idempotency response");
        }
        repository.save(record);
        LOGGER.info("idempotency response stored endpoint={} idempotencyKey={} paymentId={} statusCode={}",
                endpoint, idempotencyKey, paymentId, statusCode);
        redisTemplate.opsForValue().set(
                "idempotency:" + idempotencyKey,
                paymentId.toString(),
                Duration.ofHours(properties.ttlHours())
        );
    }

    public String hashRequest(Object request) {
        try {
            String payload = objectMapper.writeValueAsString(request);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (JsonProcessingException | NoSuchAlgorithmException ex) {
            throw new IdempotencyConflictException("Unable to hash idempotent request");
        }
    }
}
