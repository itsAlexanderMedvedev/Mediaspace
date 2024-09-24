package com.amedvedev.mediaspace.auth;

import com.amedvedev.mediaspace.exception.dto.GeneralErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;

@Slf4j
@Component
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException {
        log.warn("Authentication failed: {}", authException.getMessage());
        setResponseParams(response, "application/json", HttpServletResponse.SC_UNAUTHORIZED);
        writeErrorMessageToResponse(response, authException);
    }

    private void setResponseParams(HttpServletResponse response, String contentType, int status) {
        response.setContentType(contentType);
        response.setStatus(status);
    }

    private void writeErrorMessageToResponse(HttpServletResponse response, AuthenticationException authException)
            throws IOException {
        var mapper = getObjectMapper();
        GeneralErrorResponse errorResponse = new GeneralErrorResponse(authException.getMessage(), LocalDateTime.now());

        String jsonResponse = mapper.writeValueAsString(errorResponse);

        PrintWriter writer = response.getWriter();
        writer.write(jsonResponse);
        writer.flush();
    }

    private ObjectMapper getObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }
}
