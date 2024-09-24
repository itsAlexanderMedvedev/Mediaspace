package com.amedvedev.mediaspace.exception.handler;

import com.amedvedev.mediaspace.exception.dto.GeneralErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.MalformedJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.DisabledException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class FilterChainExceptionHandler extends OncePerRequestFilter {

    private final ObjectMapper objectMapper;
    private final GlobalExceptionHandler globalExceptionHandler;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws IOException {
        try {
            filterChain.doFilter(request, response);
        } catch (JwtException | DisabledException ex) {
            log.warn("Exception in filter chain: {}, with message: {}", ex.getClass().getSimpleName(), ex.getMessage());
            var message = getErrorMessage(ex);

            setResponseParams(response, "application/json", HttpStatus.UNAUTHORIZED);
            writeErrorMessageToResponse(response, message);
        } catch (Exception ex) {
            setResponseParams(response, "application/json", HttpStatus.INTERNAL_SERVER_ERROR);
            writeErrorMessageToResponse(response, ex.getMessage());
        }
    }

    private void writeErrorMessageToResponse(HttpServletResponse response, String message) throws IOException {
        String jsonResponse = objectMapper.writeValueAsString(
                new GeneralErrorResponse(message, LocalDateTime.now()));

        PrintWriter writer = response.getWriter();
        writer.write(jsonResponse);
        writer.flush();
    }

    private String getErrorMessage(RuntimeException ex) {

        return switch (ex) {
            case MalformedJwtException ignored -> "Malformed JWT";
            case DisabledException ignored ->
                    "Your account is deleted. If you want to restore it - use /api/users/restore endpoint.";
            default -> ex.getMessage();
        };
    }

    private void setResponseParams(HttpServletResponse response, String contentType, HttpStatus unauthorized) {
        response.setStatus(unauthorized.value());
        response.setContentType(contentType);
    }
}
