package com.javaproject.application.service.impl;

import com.javaproject.application.dto.SecurityConfigDto;
import com.javaproject.application.dto.request.BaseRequest;
import com.javaproject.application.dto.request.RegisterUserRequest;
import com.javaproject.application.dto.response.ApiResponse;
import com.javaproject.application.dto.response.RegisterUserResponse;
import com.javaproject.application.exception.custom.DBException;
import com.javaproject.application.exception.custom.UserAlreadyExistsException;
import com.javaproject.application.mapper.Mapper;
import com.javaproject.application.model.PasswordHistory;
import com.javaproject.application.model.User;
import com.javaproject.application.repository.PasswordHistoryRepository;
import com.javaproject.application.repository.UserRepository;
import com.javaproject.application.service.ProcessRequest;
import com.javaproject.application.util.PasswordUtility;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class RegisterUserService implements ProcessRequest {

    private final UserRepository userRepository;
    private final SecurityPolicyService securityPolicyService;
    private final PasswordHistoryRepository passwordHistoryRepository;
    private final RegistrationOtpService registrationOtpService;
    private final UserEventPublisher userEventPublisher;

    @Value("${app.security.policy.enable:DEFAULT}")
    private String enableSecurityPolicy;

    @Value("${app.security.password.algo:BCRYPT}")
    private String passwordAlgo;

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public ApiResponse<RegisterUserResponse> processApiRequest(BaseRequest baseRequest) {

        log.info("RegisterUserService: Processing registration request for user: {}", baseRequest.toString());
        ApiResponse<RegisterUserResponse> apiResponse = new ApiResponse<>();
        RegisterUserRequest registerUserRequest = (RegisterUserRequest) baseRequest;
        try {
            User existingInactive = resolveExistingInactiveUser(registerUserRequest);

            User savedUser;
            if (existingInactive != null) {
                // Unverified account exists — update it in place and re-issue OTP.
                savedUser = updateExistingUser(existingInactive, registerUserRequest);
                log.info("Re-registration: updated existing unverified user id={}", savedUser.getId());
            } else {
                SecurityConfigDto securityConfigDto = securityPolicyService.getByConfigId(enableSecurityPolicy);
                User newUser = setUser(registerUserRequest, securityConfigDto);
                savedUser = userRepository.save(newUser);
                PasswordHistory passwordHistory = setPasswordHistory(newUser);
                passwordHistoryRepository.save(passwordHistory);
            }

            publishRegistrationOtpNotification(savedUser, baseRequest);
            userEventPublisher.publishUserRegistered(savedUser);

            apiResponse.setSuccess(true);
            apiResponse.setResponseMessage("User registered successfully.");
            apiResponse.setRequestId(baseRequest.getRequestId());
            apiResponse.setTimestamp(OffsetDateTime.now());
            apiResponse.setResponseCode(HttpStatus.OK.toString());
            apiResponse.setData(RegisterUserResponse.builder()
                    .userId(savedUser.getId())
                    .email(savedUser.getEmail())
                    .mobile(savedUser.getMobile())
                    .build());
        } catch (UserAlreadyExistsException e) {
            log.error("User already exists: {}", e.getMessage());
            apiResponse.setSuccess(false);
            apiResponse.setResponseMessage(e.getMessage());
            apiResponse.setRequestId(baseRequest.getRequestId());
            apiResponse.setTimestamp(OffsetDateTime.now());
            apiResponse.setResponseCode(HttpStatus.BAD_REQUEST.toString());
            return apiResponse;
        } catch (DBException dbe) {
            log.error("Database error occurred while processing registration request: {}", dbe.getMessage());
            apiResponse.setSuccess(false);
            apiResponse.setResponseMessage("A database error occurred while processing the registration request.");
            apiResponse.setRequestId(baseRequest.getRequestId());
            apiResponse.setTimestamp(OffsetDateTime.now());
            apiResponse.setResponseCode(HttpStatus.INTERNAL_SERVER_ERROR.toString());
            return apiResponse;
        } catch (Exception e) {
            log.error("Error occurred while processing registration request: {}", e.getMessage());
            apiResponse.setSuccess(false);
            apiResponse.setResponseMessage("An error occurred while processing the registration request.");
            apiResponse.setRequestId(baseRequest.getRequestId());
            apiResponse.setTimestamp(OffsetDateTime.now());
            apiResponse.setResponseCode(HttpStatus.INTERNAL_SERVER_ERROR.toString());
            return apiResponse;
        }
        return apiResponse;
    }

    /**
     * Returns an existing unverified user that can be safely overwritten, or null
     * if no such user exists and a fresh account should be created.
     *
     * Throws UserAlreadyExistsException when the email or mobile belongs to an
     * active (verified) account, or when the two credentials point to two
     * different unverified accounts (ambiguous merge).
     */
    private User resolveExistingInactiveUser(RegisterUserRequest req) {
        User byEmail = isNotBlank(req.getEmail())
                ? userRepository.getByEmail(req.getEmail().trim()).orElse(null)
                : null;
        User byMobile = isNotBlank(req.getMobile())
                ? userRepository.getByMobile(req.getMobile().trim()).orElse(null)
                : null;

        if (byEmail != null && byEmail.isActive()) {
            throw new UserAlreadyExistsException("A user with the provided email already exists.");
        }
        if (byMobile != null && byMobile.isActive()) {
            throw new UserAlreadyExistsException("A user with the provided mobile already exists.");
        }

        // Both credentials hit two different inactive accounts — can't safely merge.
        if (byEmail != null && byMobile != null && !byEmail.getId().equals(byMobile.getId())) {
            throw new UserAlreadyExistsException("The email and mobile are each registered to different unverified accounts. Please contact support.");
        }

        // Return whichever inactive match we found (email takes precedence if both present).
        if (byEmail != null) return byEmail;
        return byMobile;
    }

    private User updateExistingUser(User existing, RegisterUserRequest req) {
        SecurityConfigDto securityConfigDto = securityPolicyService.getByConfigId(enableSecurityPolicy);
        OffsetDateTime now = OffsetDateTime.now();

        existing.setEmail(normalize(req.getEmail()));
        existing.setMobile(normalize(req.getMobile()));
        existing.setPasswordHash(PasswordUtility.hashPassword(req.getPassword()));
        existing.setPasswordAlgo(passwordAlgo);
        existing.setMfaEnabled(securityConfigDto.isMfaRequired());
        existing.setMfaMethod(req.getMfaMethod());
        existing.setAccountExpiresAt(now.plusDays(90));
        existing.setPasswordChangedAt(now);
        existing.setPasswordExpiresAt(now.plusDays(securityConfigDto.getPasswordMaxAgeDays()));
        existing.setLockedUntil(null);
        existing.setLockReason(null);
        existing.setFailedLoginCount(0);
        existing.setLastFailedLoginAt(null);
        existing.setUpdatedAt(now);

        User saved = userRepository.save(existing);
        passwordHistoryRepository.save(setPasswordHistory(saved));
        return saved;
    }

    private User setUser(RegisterUserRequest registerUserRequest, SecurityConfigDto securityConfigDto) {
        return User.builder()
                .email(normalize(registerUserRequest.getEmail()))
                .mobile(normalize(registerUserRequest.getMobile()))
                .passwordHash(PasswordUtility.hashPassword(registerUserRequest.getPassword()))
                .passwordAlgo(passwordAlgo)
                .isActive(false)
                .accountExpiresAt(OffsetDateTime.now().plusDays(90))
                .lockedUntil(null)
                .lockReason(null)
                .isMfaEnabled(securityConfigDto.isMfaRequired())
                .mfaMethod(registerUserRequest.getMfaMethod())
                .authenticationMethod("JWT")
                .failedLoginCount(0)
                .lastFailedLoginAt(null)
                .lastLoginAt(null)
                .passwordChangedAt(OffsetDateTime.now())
                .passwordExpiresAt(OffsetDateTime.now().plusDays(securityConfigDto.getPasswordMaxAgeDays()))
                .mustChangePassword(false)
                .securityPolicy(Mapper.toEntity(securityConfigDto))
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
    }

    private PasswordHistory setPasswordHistory(User user){
        return PasswordHistory.builder()
                .user(user)
                .passwordHash(user.getPasswordHash())
                .passwordAlgo(user.getPasswordAlgo())
                .changedAt(OffsetDateTime.now())
                .build();
    }

    private void publishRegistrationOtpNotification(User savedUser, BaseRequest baseRequest) {
        // invalidateExistingTokens=true so stale OTPs from any prior attempt are voided.
        registrationOtpService.issueOtp(savedUser, baseRequest, "auth-register", true, null);
    }

    private boolean isNotBlank(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String normalize(String value) {
        return isNotBlank(value) ? value.trim() : null;
    }
}
