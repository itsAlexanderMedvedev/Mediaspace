package com.amedvedev.mediaspace.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoSession;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.io.IOException;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class JwtAuthenticationFilterTest {

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Mock
    private JwtService jwtService;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @Mock
    private UserDetails userDetails;

    MockitoSession mockitoSession;

    @BeforeEach
    void setUp() {
        mockitoSession = Mockito.mockitoSession()
                .initMocks(this)
                .strictness(Strictness.STRICT_STUBS)
                .startMocking();

        SecurityContextHolder.clearContext();
    }

    @AfterEach
    public void tearDown() {
        mockitoSession.finishMocking();
    }

    @Test
    void doFilterInternalValidJwtUserAuthenticatedSuccessfully() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn("Bearer valid_jwt");
        when(jwtService.extractUsername("valid_jwt")).thenReturn("username");
        when(userDetailsService.loadUserByUsername("username")).thenReturn(userDetails);
        when(jwtService.isTokenValid("valid_jwt", userDetails)).thenReturn(true);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        UsernamePasswordAuthenticationToken authentication =
                (UsernamePasswordAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();


        assertThat(authentication.getCredentials()).isNull();
        assertThat(authentication.getPrincipal()).isEqualTo(userDetails);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternalInvalidJwtUserNotAuthenticated() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn("Bearer invalid_jwt");
        when(jwtService.extractUsername("invalid_jwt")).thenReturn("username");
        when(userDetailsService.loadUserByUsername("username")).thenReturn(userDetails);
        when(jwtService.isTokenValid("invalid_jwt", userDetails)).thenReturn(false);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternalNoAuthorizationHeader() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn(null);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternalAuthorizationHeaderWithoutBearer() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn("Basic NotJWT");

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternalUsernameNotFound() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn("Bearer valid_jwt");
        when(jwtService.extractUsername("valid_jwt")).thenReturn("username");
        when(userDetailsService.loadUserByUsername("username")).thenThrow(new UsernameNotFoundException("User not found"));

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("username"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("User not found");

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternalValidJwtAlreadyAuthenticated() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn("Bearer valid_jwt");
        when(jwtService.extractUsername("valid_jwt")).thenReturn("username");

        SecurityContextHolder.getContext().setAuthentication(mock(UsernamePasswordAuthenticationToken.class));

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(jwtService, never()).isTokenValid(any(), any());
        verify(filterChain).doFilter(request, response);
    }

    @ParameterizedTest
    @MethodSource("getPermittedPaths")
    void shouldNotFilterPermittedPaths(String path) {
        when(request.getRequestURI()).thenReturn(path);

        boolean result = jwtAuthenticationFilter.shouldNotFilter(request);

        assertThat(result).isTrue();
    }

    static Stream<Arguments> getPermittedPaths() {
        return Stream.of(
                Arguments.of("/error")
        );
    }

    @Test
    void shouldFilter_OtherPaths() {
        when(request.getRequestURI()).thenReturn("/some/other/path");

        boolean result = jwtAuthenticationFilter.shouldNotFilter(request);

        assertThat(result).isFalse();
    }
}
