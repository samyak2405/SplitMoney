package com.javaproject.application.controller;

import com.javaproject.application.dto.request.LoginUserRequest;
import com.javaproject.application.dto.request.GoogleOAuthLoginRequest;
import com.javaproject.application.dto.request.RegisterUserRequest;
import com.javaproject.application.dto.request.ResendRegistrationOtpRequest;
import com.javaproject.application.dto.request.VerifyRegistrationOtpRequest;
import com.javaproject.application.dto.response.ApiResponse;
import com.javaproject.application.dto.response.LoginUserResponse;
import com.javaproject.application.enums.ApiTypeEnum;
import com.javaproject.application.model.RefreshToken;
import com.javaproject.application.model.Role;
import com.javaproject.application.model.User;
import com.javaproject.application.model.UserRole;
import com.javaproject.application.repository.RefreshTokenRepository;
import com.javaproject.application.repository.UserRepository;
import com.javaproject.application.repository.UserRoleRepository;
import com.javaproject.application.security.authentication.AuthenticationResult;
import com.javaproject.application.service.ProcessRequest;
import com.javaproject.application.service.factory.ProcessorFactory;
import com.javaproject.application.service.impl.LoginService;
import com.javaproject.application.util.CookieUtil;
import com.javaproject.application.validator.Validator;
import com.javaproject.application.validator.factory.ValidatorFactory;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@RestController
public class AuthController {

    private final ValidatorFactory validatorFactory;
    private final ProcessorFactory processorFactory;
    private final LoginService loginService;
    private final CookieUtil cookieUtil;
    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    @PostMapping("/v1/register")
    public ResponseEntity<ApiResponse<?>> registerUser(@Valid @RequestBody RegisterUserRequest registerUserRequest) {
        String registrationIdentifier = registerUserRequest.getEmail() != null
                ? registerUserRequest.getEmail()
                : registerUserRequest.getMobile();
        log.info("Received registration request for identifier: {}", registrationIdentifier);
        Validator validator = validatorFactory.getValidator(ApiTypeEnum.REGISTER.getApiType());
        ProcessRequest processor = processorFactory.getProcessor(ApiTypeEnum.REGISTER.getApiType());
        log.info("Validating registration request for identifier: {}", registrationIdentifier);
        validator.validateRequest(registerUserRequest);
        log.info("Processing registration request for identifier: {}", registrationIdentifier);
        ApiResponse<?> apiResponse = processor.processApiRequest(registerUserRequest);
        log.info("Processing completed for registration request for user: {}", apiResponse.toString());
        return new ResponseEntity<>(apiResponse, HttpStatus.OK);
    }

    @PostMapping("/v1/login")
    public ResponseEntity<ApiResponse<LoginUserResponse>> loginUser(
            @Valid @RequestBody LoginUserRequest loginUserRequest,
            HttpServletResponse response
    ) {
        String loginIdentifier = loginUserRequest.getEmail() != null && !loginUserRequest.getEmail().isBlank()
                ? loginUserRequest.getEmail()
                : loginUserRequest.getMobile();
        log.info("Received login request for identifier: {}", loginIdentifier);
        Validator validator = validatorFactory.getValidator(ApiTypeEnum.LOGIN.getApiType());
        log.info("Validating login request for identifier: {}", loginIdentifier);
        validator.validateRequest(loginUserRequest);

        // Authenticate and get tokens
        AuthenticationResult authResult = loginService.authenticate(loginUserRequest);

        // Set tokens in HttpOnly cookies (NOT in response body)
        cookieUtil.addTokenCookies(response, authResult.getAccessToken(), authResult.getRefreshToken());

        // Build response body without tokens
        LoginUserResponse responseData = LoginUserResponse.builder()
                .userId(authResult.getUserId())
                .email(authResult.getEmail())
                .mobile(authResult.getMobile())
                .roles(authResult.getRoles())
                .accessTokenExpiresAt(authResult.getAccessTokenExpiresAt())
                .build();

        ApiResponse<LoginUserResponse> apiResponse = new ApiResponse<>();
        apiResponse.setRequestId(loginUserRequest.getRequestId());
        apiResponse.setSuccess(true);
        apiResponse.setResponseCode(String.valueOf(HttpStatus.OK.value()));
        apiResponse.setResponseMessage("Login successful");
        apiResponse.setTimestamp(OffsetDateTime.now());
        apiResponse.setData(responseData);

        return new ResponseEntity<>(apiResponse, HttpStatus.OK);
    }

    @PostMapping("/v1/login/google")
    public ResponseEntity<ApiResponse<LoginUserResponse>> loginWithGoogle(
            @Valid @RequestBody GoogleOAuthLoginRequest googleOAuthLoginRequest,
            HttpServletResponse response
    ) {
        log.info("Received Google OAuth login request for provider: {} request: {}", googleOAuthLoginRequest.getProvider(), googleOAuthLoginRequest);
        AuthenticationResult authResult = loginService.authenticateGoogle(googleOAuthLoginRequest);
        cookieUtil.addTokenCookies(response, authResult.getAccessToken(), authResult.getRefreshToken());

        LoginUserResponse responseData = LoginUserResponse.builder()
                .userId(authResult.getUserId())
                .email(authResult.getEmail())
                .mobile(authResult.getMobile())
                .roles(authResult.getRoles())
                .accessTokenExpiresAt(authResult.getAccessTokenExpiresAt())
                .build();

        ApiResponse<LoginUserResponse> apiResponse = new ApiResponse<>();
        apiResponse.setRequestId(googleOAuthLoginRequest.getRequestId());
        apiResponse.setSuccess(true);
        apiResponse.setResponseCode(String.valueOf(HttpStatus.OK.value()));
        apiResponse.setResponseMessage("Login successful");
        apiResponse.setTimestamp(OffsetDateTime.now());
        apiResponse.setData(responseData);

        return new ResponseEntity<>(apiResponse, HttpStatus.OK);
    }

    @PostMapping("/v1/verify-registration-otp")
    public ResponseEntity<ApiResponse<?>> verifyRegistrationOtp(
            @Valid @RequestBody VerifyRegistrationOtpRequest verifyRegistrationOtpRequest
    ) {
        String verificationIdentifier = verifyRegistrationOtpRequest.getEmail() != null
                ? verifyRegistrationOtpRequest.getEmail()
                : verifyRegistrationOtpRequest.getMobile();
        log.info("Received registration OTP verification request for identifier: {}", verificationIdentifier);
        Validator validator = validatorFactory.getValidator(ApiTypeEnum.VERIFY_REGISTRATION_OTP.getApiType());
        ProcessRequest processor = processorFactory.getProcessor(ApiTypeEnum.VERIFY_REGISTRATION_OTP.getApiType());
        validator.validateRequest(verifyRegistrationOtpRequest);
        ApiResponse<?> apiResponse = processor.processApiRequest(verifyRegistrationOtpRequest);
        return new ResponseEntity<>(apiResponse, HttpStatus.OK);
    }

    @PostMapping("/v1/resend-registration-otp")
    public ResponseEntity<ApiResponse<?>> resendRegistrationOtp(
            @Valid @RequestBody ResendRegistrationOtpRequest resendRegistrationOtpRequest
    ) {
        String resendIdentifier = resendRegistrationOtpRequest.getEmail() != null
                ? resendRegistrationOtpRequest.getEmail()
                : resendRegistrationOtpRequest.getMobile();
        log.info("Received resend registration OTP request for identifier: {}", resendIdentifier);
        Validator validator = validatorFactory.getValidator(ApiTypeEnum.RESEND_REGISTRATION_OTP.getApiType());
        ProcessRequest processor = processorFactory.getProcessor(ApiTypeEnum.RESEND_REGISTRATION_OTP.getApiType());
        validator.validateRequest(resendRegistrationOtpRequest);
        ApiResponse<?> apiResponse = processor.processApiRequest(resendRegistrationOtpRequest);
        return new ResponseEntity<>(apiResponse, HttpStatus.OK);
    }

    @PostMapping("/v1/logout")
    public ResponseEntity<ApiResponse<Object>> logoutUser(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        // Revoke the refresh token in the DB so it cannot be reused even if captured
        cookieUtil.getCookieValue(request, CookieUtil.REFRESH_TOKEN_COOKIE)
                .filter(raw -> !raw.isBlank())
                .ifPresent(rawToken -> {
                    try {
                        String tokenHash = sha256Hex(rawToken);
                        refreshTokenRepository.findByTokenHash(tokenHash).ifPresent(rt -> {
                            rt.setRevokedAt(OffsetDateTime.now());
                            refreshTokenRepository.save(rt);
                            log.info("logout: refresh token revoked for user={}", rt.getUser().getId());
                        });
                    } catch (Exception e) {
                        log.warn("logout: could not revoke refresh token — {}", e.getMessage());
                    }
                });

        cookieUtil.clearTokenCookies(response);

        ApiResponse<Object> apiResponse = new ApiResponse<>();
        apiResponse.setSuccess(true);
        apiResponse.setResponseCode(String.valueOf(HttpStatus.OK.value()));
        apiResponse.setResponseMessage("Logout successful");
        apiResponse.setTimestamp(OffsetDateTime.now());

        return new ResponseEntity<>(apiResponse, HttpStatus.OK);
    }

    private String sha256Hex(String value) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : hash) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /**
     * Returns the current authenticated session based on the JWT in cookies/headers.
     * Used by the SPA to restore session after page refresh without exposing tokens to JS.
     */
    @GetMapping("/v1/session")
    public ResponseEntity<ApiResponse<LoginUserResponse>> getCurrentSession(HttpServletRequest request) {
        ApiResponse<LoginUserResponse> apiResponse = new ApiResponse<>();
        apiResponse.setTimestamp(OffsetDateTime.now());

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication.getPrincipal() == null
                || "anonymousUser".equals(authentication.getPrincipal())) {
            apiResponse.setSuccess(false);
            apiResponse.setResponseCode(String.valueOf(HttpStatus.UNAUTHORIZED.value()));
            apiResponse.setResponseMessage("Not authenticated");
            return new ResponseEntity<>(apiResponse, HttpStatus.UNAUTHORIZED);
        }

        Object userIdAttr = request.getAttribute("userId");
        if (!(userIdAttr instanceof UUID userId)) {
            apiResponse.setSuccess(false);
            apiResponse.setResponseCode(String.valueOf(HttpStatus.UNAUTHORIZED.value()));
            apiResponse.setResponseMessage("Not authenticated");
            return new ResponseEntity<>(apiResponse, HttpStatus.UNAUTHORIZED);
        }

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            apiResponse.setSuccess(false);
            apiResponse.setResponseCode(String.valueOf(HttpStatus.UNAUTHORIZED.value()));
            apiResponse.setResponseMessage("User not found");
            return new ResponseEntity<>(apiResponse, HttpStatus.UNAUTHORIZED);
        }

        List<String> roles = userRoleRepository.findByUser(user).stream()
                .map(UserRole::getRole)
                .map(Role::getName)
                .collect(Collectors.toList());

        LoginUserResponse responseData = LoginUserResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .mobile(user.getMobile())
                .roles(roles)
                .accessTokenExpiresAt(null)
                .build();

        apiResponse.setSuccess(true);
        apiResponse.setResponseCode(String.valueOf(HttpStatus.OK.value()));
        apiResponse.setResponseMessage("Session active");
        apiResponse.setData(responseData);

        return new ResponseEntity<>(apiResponse, HttpStatus.OK);
    }

    @PostMapping("/v1/forgot-password")
    public ResponseEntity<ApiResponse<?>> forgotPassword(
            @Valid @RequestBody com.javaproject.application.dto.request.ForgotPasswordRequest request
    ) {
        validatorFactory.getValidator(ApiTypeEnum.FORGOT_PASSWORD.getApiType()).validateRequest(request);
        ApiResponse<?> response = processorFactory.getProcessor(ApiTypeEnum.FORGOT_PASSWORD.getApiType()).processApiRequest(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/v1/reset-password")
    public ResponseEntity<ApiResponse<?>> resetPassword(
            @Valid @RequestBody com.javaproject.application.dto.request.ResetPasswordRequest request
    ) {
        validatorFactory.getValidator(ApiTypeEnum.RESET_PASSWORD.getApiType()).validateRequest(request);
        ApiResponse<?> response = processorFactory.getProcessor(ApiTypeEnum.RESET_PASSWORD.getApiType()).processApiRequest(request);
        return ResponseEntity.ok(response);
    }
}
