package com.amedvedev.mediaspace.exception.handler;

import com.amedvedev.mediaspace.exception.dto.GeneralErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.MalformedJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.DisabledException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class FilterChainExceptionHandler extends OncePerRequestFilter {

    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws IOException {
        System.out.println("--- HERE FilterChainExceptionHandler ---");
        try {
            filterChain.doFilter(request, response);
        } catch (JwtException ex) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType("application/json");

            String message;
            if (ex instanceof MalformedJwtException) {
                message = "Malformed JWT";
            } else {
                message = ex.getMessage();
            }

            String jsonResponse = objectMapper.writeValueAsString(
                    new GeneralErrorResponse(message, LocalDateTime.now()));

            PrintWriter writer = response.getWriter();
            writer.write(jsonResponse);
            writer.flush();
        } catch (Exception ex) {
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.setContentType("application/json");

            String jsonResponse = objectMapper.writeValueAsString(
                    new GeneralErrorResponse(ex.getMessage(), LocalDateTime.now()));

            PrintWriter writer = response.getWriter();
            writer.write(jsonResponse);
            writer.flush();
        }
    }
}
