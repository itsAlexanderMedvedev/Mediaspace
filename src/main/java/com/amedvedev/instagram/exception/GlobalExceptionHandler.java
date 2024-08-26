package com.amedvedev.instagram.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, Object> response = new LinkedHashMap<>();
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        var badRequest = HttpStatus.BAD_REQUEST;
        response.put("status", badRequest.value() + " " + badRequest.getReasonPhrase());
        response.put("errors", errors);
        response.put("timestamp", new Date());
        return new ResponseEntity<>(response, badRequest);
    }

    @ExceptionHandler(UsernameAlreadyExistsException.class)
    public ResponseEntity<Map<String, Object>> handleUsernameAlreadyExistsException(UsernameAlreadyExistsException ex) {
        Map<String, Object> response = new LinkedHashMap<>();
        var conflict = HttpStatus.CONFLICT;
        response.put("status", conflict.value() + " " + conflict.getReasonPhrase());
        response.put("reason", ex.getMessage());
        response.put("timestamp", new Date());
        return new ResponseEntity<>(response, conflict);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, Object>> handleAuthenticationException(Exception ex) {
        Map<String, Object> response = new LinkedHashMap<>();
        var unauthorized = HttpStatus.UNAUTHORIZED;
        response.put("status", unauthorized.value() + " " + unauthorized.getReasonPhrase());
        response.put("reason", ex.getMessage());
        response.put("timestamp", new Date());
        return new ResponseEntity<>(response, unauthorized);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(IllegalArgumentException ex) {
        Map<String, Object> response = new LinkedHashMap<>();
        var badRequest = HttpStatus.BAD_REQUEST;
        response.put("status", badRequest.value() + " " + badRequest.getReasonPhrase());
        response.put("reason", ex.getMessage());
        response.put("timestamp", new Date());
        return new ResponseEntity<>(response, badRequest);
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleUserNotFoundException(UserNotFoundException ex) {
        Map<String, Object> response = new LinkedHashMap<>();
        var notFound = HttpStatus.NOT_FOUND;
        response.put("status", notFound.value() + " " + notFound.getReasonPhrase());
        response.put("reason", ex.getMessage());
        response.put("timestamp", new Date());
        return new ResponseEntity<>(response, notFound);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleException(Exception ex) {
        Map<String, Object> response = new LinkedHashMap<>();
        var internalServerError = HttpStatus.INTERNAL_SERVER_ERROR;
        response.put("status", internalServerError.value() + " " + internalServerError.getReasonPhrase());
        response.put("reason", ex.getMessage());
        response.put("timestamp", new Date());
        return new ResponseEntity<>(response, internalServerError);
    }
}