package com.javaproject.application.service.impl;

import com.javaproject.application.dto.notification.NotificationEventMessage;
import com.javaproject.application.dto.notification.RegistrationOtpPayload;
import com.javaproject.application.dto.request.BaseRequest;
import com.javaproject.application.dto.request.ForgotPasswordRequest;
import com.javaproject.application.dto.response.ApiResponse;
import com.javaproject.application.dto.response.ForgotPasswordResponse;
import com.javaproject.application.enums.NotificationChannel;
import com.javaproject.application.enums.NotificationEventType;
import com.javaproject.application.exception.custom.ProcessApiException;
import com.javaproject.application.model.User;
import com.javaproject.application.repository.UserRepository;
import com.javaproject.application.service.ProcessRequest;
import com.javaproject.application.service.otp.OtpStore;
import com.javaproject.application.util.PasswordUtility;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ForgotPasswordService implements ProcessRequest {

    static final String PASSWORD_RESET_PURPOSE = "PASSWORD_RESET";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final OtpStore otpStore;
    private final NotificationEventPublisher notificationEventPublisher;

    @Value("${app.notifications.registration-otp.expiry-minutes:10}")
    private int otpExpiryMinutes;

    @Override
    @Transactional
    public ApiResponse<ForgotPasswordResponse> processApiRequest(BaseRequest baseRequest) {
        ForgotPasswordRequest request = (ForgotPasswordRequest) baseRequest;

        User user = resolveUser(request);

        NotificationChannel channel = resolveChannel(user, request.getOtpChannel());
        String otp = generateOtp();
        OffsetDateTime issuedAt = OffsetDateTime.now();
        OffsetDateTime expiresAt = issuedAt.plusMinutes(otpExpiryMinutes);

        otpStore.issue(OtpStore.OtpIssueRequest.builder()
                .user(user)
                .purpose(PASSWORD_RESET_PURPOSE)
                .tokenHash(PasswordUtility.hashToken(otp))
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .resendCooldownSeconds(60)
                .ipAddress(request.getIpAddress())
                .deliveryChannel(channel.name())
                .invalidateExistingTokens(true)
                .build());

        notificationEventPublisher.publishRegistrationOtpEvent(
                NotificationEventMessage.builder()
                        .notificationId(UUID.randomUUID().toString())
                        .userId(user.getId().toString())
                        .channel(channel)
                        .eventType(NotificationEventType.PASSWORD_RESET_OTP)
                        .idempotencyKey("pwd-reset-" + request.getRequestId())
                        .attempt(1)
                        .createdAt(Instant.now().toString())
                        .payload(RegistrationOtpPayload.builder()
                                .email(user.getEmail())
                                .mobile(user.getMobile())
                                .otp(otp)
                                .expiryMinutes(otpExpiryMinutes)
                                .build())
                        .build(),
                request.getRequestId()
        );

        log.info("Password reset OTP issued for user={} channel={}", user.getEmail(), channel);

        ApiResponse<ForgotPasswordResponse> response = new ApiResponse<>();
        response.setRequestId(request.getRequestId());
        response.setSuccess(true);
        response.setResponseCode(String.valueOf(HttpStatus.OK.value()));
        response.setResponseMessage("OTP sent successfully.");
        response.setTimestamp(OffsetDateTime.now());
        response.setData(ForgotPasswordResponse.builder()
                .deliveryChannel(channel.name())
                .expiresAt(expiresAt)
                .build());
        return response;
    }

    private User resolveUser(ForgotPasswordRequest request) {
        if (isPresent(request.getEmail())) {
            return userRepository.getByEmail(request.getEmail().trim())
                    .orElseThrow(() -> new ProcessApiException("No account found with this email.", HttpStatus.NOT_FOUND));
        }
        if (isPresent(request.getMobile())) {
            return userRepository.getByMobile(request.getMobile().trim())
                    .orElseThrow(() -> new ProcessApiException("No account found with this mobile.", HttpStatus.NOT_FOUND));
        }
        throw new ProcessApiException("Email or mobile is required.", HttpStatus.BAD_REQUEST);
    }

    private NotificationChannel resolveChannel(User user, String preferredChannel) {
        if ("SMS".equalsIgnoreCase(preferredChannel) && isPresent(user.getMobile())) {
            return NotificationChannel.SMS;
        }
        if ("EMAIL".equalsIgnoreCase(preferredChannel) && isPresent(user.getEmail())) {
            return NotificationChannel.EMAIL;
        }
        if (isPresent(user.getEmail())) return NotificationChannel.EMAIL;
        if (isPresent(user.getMobile())) return NotificationChannel.SMS;
        throw new ProcessApiException("No delivery channel available for this account.", HttpStatus.UNPROCESSABLE_ENTITY);
    }

    private String generateOtp() {
        return String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));
    }

    private boolean isPresent(String value) {
        return value != null && !value.isBlank();
    }
}
