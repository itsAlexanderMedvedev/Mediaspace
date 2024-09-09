package com.amedvedev.mediaspace.auth;

import com.amedvedev.mediaspace.auth.dto.LoginRequest;
import com.amedvedev.mediaspace.auth.dto.LoginResponse;
import com.amedvedev.mediaspace.auth.dto.RegisterRequest;
import com.amedvedev.mediaspace.user.exception.UsernameAlreadyExistsException;
import com.amedvedev.mediaspace.user.User;
import com.amedvedev.mediaspace.user.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoSession;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;

class AuthenticationServiceTest {

    @InjectMocks
    private AuthenticationService authenticationService;

    @Mock
    private UserService userService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    MockitoSession mockitoSession;

    @BeforeEach
    public void setUp() {
        mockitoSession = Mockito.mockitoSession()
                .initMocks(this)
                .strictness(Strictness.STRICT_STUBS)
                .startMocking();
    }

    @AfterEach
    public void tearDown() {
        mockitoSession.finishMocking();
    }

    @Test
    void registerUserSuccess() {
        var request = new RegisterRequest("username", "password");


        Mockito.when(userService.findByUsernameIgnoreCaseAndIncludeSoftDeleted(request.getUsername()))
                .thenReturn(Optional.empty());
        Mockito.when(passwordEncoder.encode(request.getPassword()))
                .thenReturn("encoded-password");

        // When
        authenticationService.register(request);

        // Then
        verify(userService).save(argThat(user ->
                "username".equals(user.getUsername()) && "encoded-password".equals(user.getPassword())
        ));
    }

    @Test
    void registerUserExists() {
        var request = new RegisterRequest("username", "password");


        Mockito.when(userService.findByUsernameIgnoreCaseAndIncludeSoftDeleted(request.getUsername()))
                .thenReturn(Optional.of(new User()));


        assertThrows(UsernameAlreadyExistsException.class,
                () -> authenticationService.register(request));
    }


    @Test
    void loginInvalidCredentialsThrowsException() {
        var request = new LoginRequest("username", "password");


        Mockito.when(authenticationManager.authenticate(Mockito.any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(BadCredentialsException.class);


        assertThrows(BadCredentialsException.class, () -> authenticationService.login(request));
    }


    @Test
    void loginServiceSuccess() {
        var request = new LoginRequest("username", "password");


        Mockito.when(authenticationManager.authenticate(Mockito.any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(new UsernamePasswordAuthenticationToken("username", "password"));
        Mockito.when(userService.findByUsernameIgnoreCaseAndIncludeSoftDeleted(request.getUsername()))
                .thenReturn(
                        Optional.of(
                                User.builder()
                                        .username(request.getUsername())
                                        .password("encoded-password")
                                        .build()
                        )
                );
        Mockito.when(jwtService.generateToken(Mockito.any(User.class)))
                .thenReturn("mock-token");


        LoginResponse response = authenticationService.login(request);

        assertThat(response.getToken()).isEqualTo("mock-token");
    }
}