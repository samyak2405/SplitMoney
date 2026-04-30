package com.javaproject.application.service.impl;

import com.javaproject.application.dto.request.BaseRequest;
import com.javaproject.application.dto.request.ResetPasswordRequest;
import com.javaproject.application.dto.response.ApiResponse;
import com.javaproject.application.dto.response.ResetPasswordResponse;
import com.javaproject.application.exception.custom.ProcessApiException;
import com.javaproject.application.model.PasswordHistory;
import com.javaproject.application.model.RefreshToken;
import com.javaproject.application.model.User;
import com.javaproject.application.repository.PasswordHistoryRepository;
import com.javaproject.application.repository.RefreshTokenRepository;
import com.javaproject.application.repository.UserRepository;
import com.javaproject.application.service.ProcessRequest;
import com.javaproject.application.service.impl.SecurityPolicyService;
import com.javaproject.application.service.otp.OtpStore;
import com.javaproject.application.util.PasswordUtility;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResetPasswordService implements ProcessRequest {

    private final UserRepository userRepository;
    private final OtpStore otpStore;
    private final PasswordHistoryRepository passwordHistoryRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final SecurityPolicyService securityPolicyService;

    @Value("${app.notifications.registration-otp.max-attempts:5}")
    private int maxOtpAttempts;

    @Value("${app.notifications.registration-otp.cooldown-minutes:15}")
    private int cooldownMinutes;

    @Value("${app.security.policy.enable:DEFAULT}")
    private String securityPolicyName;

    @Value("${app.security.password.algo:BCRYPT}")
    private String passwordAlgo;

    @Override
    @Transactional
    public ApiResponse<ResetPasswordResponse> processApiRequest(BaseRequest baseRequest) {
        ResetPasswordRequest request = (ResetPasswordRequest) baseRequest;

        User user = resolveUser(request);
        OffsetDateTime now = OffsetDateTime.now();

        // Verify OTP
        OtpStore.OtpVerifyResult verifyResult = otpStore.verify(OtpStore.OtpVerifyRequest.builder()
                .user(user)
                .purpose(ForgotPasswordService.PASSWORD_RESET_PURPOSE)
                .providedTokenHash(PasswordUtility.hashToken(request.getOtp()))
                .maxAttempts(maxOtpAttempts)
                .now(now)
                .build());

        switch (verifyResult.getStatus()) {
            case NOT_FOUND ->
                throw new ProcessApiException("OTP not found. Request a new one.", HttpStatus.BAD_REQUEST);
            case EXPIRED ->
                throw new ProcessApiException("OTP has expired. Request a new one.", HttpStatus.BAD_REQUEST);
            case INVALID ->
                throw new ProcessApiException("Invalid OTP. Please check and try again.", HttpStatus.UNAUTHORIZED);
            case ATTEMPTS_EXCEEDED -> {
                OffsetDateTime lockedUntil = now.plusMinutes(cooldownMinutes);
                user.setLockedUntil(lockedUntil);
                user.setLockReason("PASSWORD_RESET_OTP_ATTEMPTS_EXCEEDED");
                user.setUpdatedAt(now);
                userRepository.save(user);
                throw new ProcessApiException(
                        "Too many invalid attempts. Try again after " + cooldownMinutes + " minutes.",
                        HttpStatus.FORBIDDEN);
            }
            case VERIFIED -> { /* proceed */ }
        }

        // Validate password policy
        int minLength = securityPolicyService.getByConfigId(securityPolicyName).getPasswordMinLength();
        if (request.getNewPassword().length() < minLength) {
            throw new ProcessApiException(
                    "Password must be at least " + minLength + " characters.", HttpStatus.BAD_REQUEST);
        }

        // Update password
        String newHash = PasswordUtility.hashPassword(request.getNewPassword());
        user.setPasswordHash(newHash);
        user.setPasswordAlgo(passwordAlgo);
        user.setPasswordChangedAt(now);
        user.setMustChangePassword(false);
        user.setUpdatedAt(now);
        userRepository.save(user);

        // Save to password history
        passwordHistoryRepository.save(PasswordHistory.builder()
                .user(user)
                .passwordHash(newHash)
                .passwordAlgo(passwordAlgo)
                .changedAt(now)
                .build());

        // Revoke all active refresh tokens so existing sessions are invalidated
        List<RefreshToken> activeTokens = refreshTokenRepository.findByUserAndRevokedAtIsNull(user);
        activeTokens.forEach(t -> t.setRevokedAt(now));
        refreshTokenRepository.saveAll(activeTokens);

        log.info("Password reset successful for user={}", user.getEmail());

        ApiResponse<ResetPasswordResponse> response = new ApiResponse<>();
        response.setRequestId(request.getRequestId());
        response.setSuccess(true);
        response.setResponseCode(String.valueOf(HttpStatus.OK.value()));
        response.setResponseMessage("Password reset successfully. Please sign in with your new password.");
        response.setTimestamp(now);
        response.setData(ResetPasswordResponse.builder().changedAt(now).build());
        return response;
    }

    private User resolveUser(ResetPasswordRequest request) {
        if (isPresent(request.getEmail())) {
            return userRepository.getByEmail(request.getEmail().trim())
                    .orElseThrow(() -> new ProcessApiException("User not found.", HttpStatus.NOT_FOUND));
        }
        if (isPresent(request.getMobile())) {
            return userRepository.getByMobile(request.getMobile().trim())
                    .orElseThrow(() -> new ProcessApiException("User not found.", HttpStatus.NOT_FOUND));
        }
        throw new ProcessApiException("Email or mobile is required.", HttpStatus.BAD_REQUEST);
    }

    private boolean isPresent(String value) {
        return value != null && !value.isBlank();
    }
}
