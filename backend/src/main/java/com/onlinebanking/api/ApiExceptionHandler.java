package com.onlinebanking.api;

import java.time.Instant;
import java.util.Objects;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ApiDtos.ErrorView> handleApiException(ApiException exception) {
        return error(exception.getStatus(), exception.getMessage());
    }

    @ExceptionHandler(BadCredentialsException.class)
    ResponseEntity<ApiDtos.ErrorView> handleBadCredentials() {
        return error(HttpStatus.UNAUTHORIZED, "Email or password is incorrect.");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiDtos.ErrorView> handleValidation(MethodArgumentNotValidException exception) {
        FieldError fieldError = exception.getBindingResult().getFieldErrors().stream().findFirst().orElse(null);
        String message = fieldError == null ? "Please check the submitted details."
                : Objects.requireNonNullElse(fieldError.getDefaultMessage(), "Please check the submitted details.");
        return error(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ApiDtos.ErrorView> handleConflict() {
        return error(HttpStatus.CONFLICT, "An account with those details already exists.");
    }

    private ResponseEntity<ApiDtos.ErrorView> error(HttpStatus status, String message) {
        return ResponseEntity.status(status)
                .body(new ApiDtos.ErrorView(status.getReasonPhrase(), message, Instant.now()));
    }
}