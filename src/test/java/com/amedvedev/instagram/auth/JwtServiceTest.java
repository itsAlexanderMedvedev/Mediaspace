package com.amedvedev.instagram.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoSession;
import org.mockito.quality.Strictness;
import org.springframework.security.core.userdetails.UserDetails;

import java.security.Key;
import java.util.Base64;
import java.util.Date;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class JwtServiceTest {

    private JwtService jwtService;

    private MockitoSession mockitoSession;

    @Mock
    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        mockitoSession = Mockito.mockitoSession()
                .initMocks(this)
                .strictness(Strictness.STRICT_STUBS)
                .startMocking();

        Key secretKey = Jwts.SIG.HS256.key().build();
        String base64EncodedKey = Base64.getEncoder().encodeToString(secretKey.getEncoded());
        jwtService = new JwtService(base64EncodedKey);
    }

    @AfterEach
    void tearDown() {
        mockitoSession.finishMocking();
    }

    @Test
    void generateTokenWithExtraClaims() {
        when(userDetails.getUsername()).thenReturn("testUser");

        String token = jwtService.generateToken(Map.of("role", "admin"), userDetails);

        assertThat(token).isNotNull();
        assertThat(token).startsWith("eyJ");
    }

    @Test
    void generateTokenWithoutExtraClaims() {
        when(userDetails.getUsername()).thenReturn("testUser");

        String token = jwtService.generateToken(userDetails);

        assertThat(token).isNotNull();
        assertThat(token).startsWith("eyJ");
    }

    @Test
    void isTokenValidReturnsTrueForValidToken() {
        when(userDetails.getUsername()).thenReturn("testUser");

        String token = jwtService.generateToken(userDetails);
        boolean isValid = jwtService.isTokenValid(token, userDetails);

        assertThat(isValid).isTrue();
    }

    @Test
    void isTokenValidReturnsFalseForInvalidToken() {
        String token = "invalidToken";

        assertThatThrownBy(() -> jwtService.isTokenValid(token, userDetails))
                .isInstanceOf(MalformedJwtException.class)
                .hasMessageStartingWith("Invalid compact JWT");
    }

    @Test
    void isTokenValidReturnFalseForExpiredToken() {
        when(userDetails.getUsername()).thenReturn("testUser");

        String token = jwtService.generateToken(userDetails, 0);

        assertThatThrownBy(() -> jwtService.isTokenValid(token, userDetails))
                .isInstanceOf(ExpiredJwtException.class)
                .hasMessageStartingWith("JWT expired");
    }

    @Test
    void isTokenValidReturnFalseForInvalidUsername() {
        when(userDetails.getUsername()).thenReturn("testUser");
        String token = jwtService.generateToken(userDetails);
        when(jwtService.extractUsername(token)).thenReturn("invalidUser");

        System.out.println(jwtService.isTokenValid(token, userDetails));

        assertThatThrownBy(() -> jwtService.isTokenValid(token, userDetails))
                .isInstanceOf(ExpiredJwtException.class)
                .hasMessageStartingWith("JWT expired");
    }


    @Test
    void extractUsernameReturnsCorrectUsername() {
        when(userDetails.getUsername()).thenReturn("testUser");

        String token = jwtService.generateToken(userDetails);
        String username = jwtService.extractUsername(token);

        assertThat(username).isEqualTo("testUser");
    }

    @Test
    void extractClaimReturnsCorrectClaim() {
        when(userDetails.getUsername()).thenReturn("testUser");

        String token = jwtService.generateToken(userDetails);
        Date expirationDate = jwtService.extractClaim(token, Claims::getExpiration);

        assertThat(expirationDate).isNotNull();
        assertThat(expirationDate).isAfter(new Date());
    }
}
