package com.amedvedev.mediaspace.exception.handler;

import com.amedvedev.mediaspace.exception.BadRequestActionException;
import com.amedvedev.mediaspace.exception.ElementNotFoundException;
import com.amedvedev.mediaspace.exception.ForbiddenActionException;
import com.amedvedev.mediaspace.exception.dto.GeneralErrorResponse;
import com.amedvedev.mediaspace.exception.dto.ValidationErrorResponse;
import com.amedvedev.mediaspace.user.exception.UsernameAlreadyExistsException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ValidationErrorResponse handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new LinkedHashMap<>();

        extractErrorsFromException(ex, errors);

        return new ValidationErrorResponse(errors, LocalDateTime.now());
    }

    private void extractErrorsFromException(MethodArgumentNotValidException ex, Map<String, String> errors) {
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
    }

    @ExceptionHandler(UsernameAlreadyExistsException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public GeneralErrorResponse handleUsernameAlreadyExistsException(UsernameAlreadyExistsException ex) {
        return new GeneralErrorResponse(ex.getMessage(), LocalDateTime.now());
    }

    @ExceptionHandler(DisabledException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public GeneralErrorResponse handleDisabledException(DisabledException ex) {
        return new GeneralErrorResponse(
                "Your account is deleted. If you want to restore it - use /api/users/restore endpoint.",
                LocalDateTime.now());
    }

    @ExceptionHandler(AuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public GeneralErrorResponse handleAuthenticationException(AuthenticationException ex) {
        return new GeneralErrorResponse(ex.getMessage(), LocalDateTime.now());
    }

    @ExceptionHandler(BadRequestActionException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public GeneralErrorResponse handleBadRequestException(RuntimeException ex) {
        return new GeneralErrorResponse(ex.getMessage(), LocalDateTime.now());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public GeneralErrorResponse handleHttpMessageNotReadableException(HttpMessageNotReadableException ex) {
        return new GeneralErrorResponse("Required request body is missing or malformed", LocalDateTime.now());
    }

    @ExceptionHandler({ElementNotFoundException.class, NoResourceFoundException.class})
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public GeneralErrorResponse handleNotFoundException(Exception ex) {
        return new GeneralErrorResponse(ex.getMessage(), LocalDateTime.now());
    }

    @ExceptionHandler(ForbiddenActionException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public GeneralErrorResponse handleForbiddenOperationException(ForbiddenActionException ex) {
        return new GeneralErrorResponse(ex.getMessage(), LocalDateTime.now());
    }

    @ExceptionHandler({HttpRequestMethodNotSupportedException.class, NoHandlerFoundException.class})
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public GeneralErrorResponse handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException ex) {
        return new GeneralErrorResponse(ex.getMessage(), LocalDateTime.now());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public GeneralErrorResponse handleException(Exception ex) {
        log.error("An unexpected error occurred: {}", ex.getMessage(), ex);
        return new GeneralErrorResponse(ex.getMessage(), LocalDateTime.now());
    }
}