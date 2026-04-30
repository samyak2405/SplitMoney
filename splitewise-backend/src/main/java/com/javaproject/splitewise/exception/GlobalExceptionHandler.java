package com.javaproject.splitewise.exception;

import com.javaproject.splitewise.dto.response.ApiResponse;
import com.javaproject.splitewise.exception.custom.ApiValidationException;
import com.javaproject.splitewise.exception.custom.GroupAdminNotFoundException;
import com.javaproject.splitewise.exception.custom.ProcessApiException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Map<String, String>> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage());
        }
        String message = fieldErrors.values().stream().findFirst().orElse("Invalid request");
        return errorResponse(request, HttpStatus.BAD_REQUEST, message, fieldErrors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Map<String, String>> handleConstraintViolation(
            ConstraintViolationException ex,
            HttpServletRequest request
    ) {
        Map<String, String> violations = new LinkedHashMap<>();
        for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
            violations.putIfAbsent(violation.getPropertyPath().toString(), violation.getMessage());
        }
        String message = violations.values().stream().findFirst().orElse("Invalid request");
        return errorResponse(request, HttpStatus.BAD_REQUEST, message, violations);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Object> handleMessageNotReadable(
            HttpMessageNotReadableException ex,
            HttpServletRequest request
    ) {
        return errorResponse(request, HttpStatus.BAD_REQUEST, "Malformed JSON request", null);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Object> handleMissingParameter(
            MissingServletRequestParameterException ex,
            HttpServletRequest request
    ) {
        return errorResponse(request, HttpStatus.BAD_REQUEST,
                "Missing request parameter: " + ex.getParameterName(), null);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Object> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request
    ) {
        return errorResponse(request, HttpStatus.BAD_REQUEST,
                "Invalid value for parameter: " + ex.getName(), null);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public ApiResponse<Object> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException ex,
            HttpServletRequest request
    ) {
        return errorResponse(request, HttpStatus.METHOD_NOT_ALLOWED, "Method not allowed", null);
    }

    @ExceptionHandler(EntityNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResponse<Object> handleEntityNotFound(
            EntityNotFoundException ex,
            HttpServletRequest request
    ) {
        return errorResponse(request, HttpStatus.NOT_FOUND, "Resource not found", null);
    }

    @ExceptionHandler(NoSuchElementException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResponse<Object> handleNoSuchElement(
            NoSuchElementException ex,
            HttpServletRequest request
    ) {
        return errorResponse(request, HttpStatus.NOT_FOUND, "Resource not found", null);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiResponse<Object> handleDataIntegrity(
            DataIntegrityViolationException ex,
            HttpServletRequest request
    ) {
        return errorResponse(request, HttpStatus.CONFLICT, "Data integrity violation", null);
    }

    @ExceptionHandler(ApiValidationException.class)
    public ResponseEntity<ApiResponse<Object>> handleApiValidation(
            ApiValidationException ex,
            HttpServletRequest request
    ) {
        HttpStatus status = ex.getStatus();
        return new ResponseEntity<>(errorResponse(request, status, ex.getMessage(), null), status);
    }

    @ExceptionHandler(ProcessApiException.class)
    public ResponseEntity<ApiResponse<Object>> handleProcessApi(
            ProcessApiException ex,
            HttpServletRequest request
    ) {
        HttpStatus status = ex.getStatus();
        return new ResponseEntity<>(errorResponse(request, status, ex.getMessage(), null), status);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Object> handleIllegalArgument(
            IllegalArgumentException ex,
            HttpServletRequest request
    ) {
        return errorResponse(request, HttpStatus.BAD_REQUEST, ex.getMessage(), null);
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiResponse<Object> handleIllegalState(
            IllegalStateException ex,
            HttpServletRequest request
    ) {
        return errorResponse(request, HttpStatus.CONFLICT, ex.getMessage(), null);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Object> handleUnexpected(
            Exception ex,
            HttpServletRequest request
    ) {
        return errorResponse(request, HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error", null);
    }

    private <T> ApiResponse<T> errorResponse(
            HttpServletRequest request,
            HttpStatus status,
            String message,
            T data
    ) {
        String requestId = MDC.get("requestId");
        log.error("Request [{}] failed — status={}, message={}", requestId, status.value(), message);

        ApiResponse<T> response = new ApiResponse<>();
        response.setRequestId(requestId);
        response.setSuccess(false);
        response.setResponseCode(String.valueOf(status.value()));
        response.setResponseMessage(message);
        response.setTimestamp(OffsetDateTime.now());
        response.setData(data);
        return response;
    }
}
