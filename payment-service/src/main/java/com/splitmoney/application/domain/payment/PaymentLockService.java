package com.splitmoney.application.domain.payment;

import java.time.Duration;
import java.util.UUID;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class PaymentLockService {

    private final StringRedisTemplate redisTemplate;

    public PaymentLockService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public boolean tryLock(UUID paymentId, Duration duration) {
        try {
            Boolean acquired = redisTemplate.opsForValue().setIfAbsent(
                    "lock:payment:" + paymentId,
                    "1",
                    duration
            );
            return Boolean.TRUE.equals(acquired);
        } catch (Exception ex) {
            // Fallback when Redis is unavailable: proceed without distributed lock.
            return true;
        }
    }

    public void unlock(UUID paymentId) {
        try {
            redisTemplate.delete("lock:payment:" + paymentId);
        } catch (Exception ignored) {
            // no-op
        }
    }
}
