package com.amedvedev.mediaspace.auth;

import com.amedvedev.mediaspace.auth.dto.LoginRequest;
import com.amedvedev.mediaspace.auth.dto.LoginResponse;
import com.amedvedev.mediaspace.auth.dto.RegisterRequest;
import com.amedvedev.mediaspace.auth.dto.RegisterResponse;
import com.amedvedev.mediaspace.user.User;
import com.amedvedev.mediaspace.user.service.UserService;
import com.amedvedev.mediaspace.user.exception.UsernameAlreadyExistsException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public RegisterResponse register(RegisterRequest request) {
        log.info("Registering user: {}", request.getUsername());

        verifyUsernameIsFree(request);

        var user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .build();
        userService.save(user);

        return new RegisterResponse("User registered successfully");
    }

    private void verifyUsernameIsFree(RegisterRequest request) {
        if (!userService.isUsernameFree(request.getUsername())) {
            log.warn("Username already exists: {}", request.getUsername());
            throw new UsernameAlreadyExistsException("This username is already taken");
        }
    }

    public LoginResponse login(LoginRequest request) {
        log.info("Logging in user: {}", request.getUsername());

        // Throws BadCredentialsException or InternalAuthenticationServiceException if authentication fails
        // (Handled by GlobalExceptionHandler)
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        var user = userService.findByUsernameIgnoreCaseAndIncludeSoftDeleted(request.getUsername());

        var token = jwtService.generateToken(user);
        return new LoginResponse(token);
    }
}
