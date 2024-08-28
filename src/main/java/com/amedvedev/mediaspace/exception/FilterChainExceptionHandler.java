package com.amedvedev.mediaspace.exception;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Date;

@Component
public class FilterChainExceptionHandler extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws IOException {
        try {
            filterChain.doFilter(request, response);
        }  catch (MalformedJwtException ex) {
            response.setStatus(HttpStatus.BAD_REQUEST.value());
            response.setContentType("application/json");
            response.getWriter().write("{\"reason\":\"Malformed JWT\", \"timestamp\":\"" + new Date() + "\"}");
        } catch (ExpiredJwtException ex) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType("application/json");
            response.getWriter().write("{\"reason\":\"Expired JWT\", \"timestamp\":\"" + new Date() + "\"}");
        } catch (SignatureException ex) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType("application/json");
            response.getWriter().write("{\"reason\":\"Invalid JWT signature\", \"timestamp\":\"" + new Date() + "\"}");
        } catch (Exception ex) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType("application/json");
            response.getWriter().write("{\"reason\":\"Invalid JWT\", \"timestamp\":\"" + new Date() + "\"}");
        }
    }
}
