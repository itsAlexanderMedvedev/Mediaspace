package com.amedvedev.mediaspace.auth;

import com.amedvedev.mediaspace.auth.dto.LoginRequest;
import com.amedvedev.mediaspace.auth.dto.LoginResponse;
import com.amedvedev.mediaspace.auth.dto.RegisterRequest;
import com.amedvedev.mediaspace.auth.dto.RegisterResponse;
import com.amedvedev.mediaspace.user.User;
import com.amedvedev.mediaspace.user.UserService;
import com.amedvedev.mediaspace.user.exception.UserNotFoundException;
import com.amedvedev.mediaspace.user.exception.UsernameAlreadyExistsException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public RegisterResponse register(RegisterRequest request) {

        if (userService.findByUsernameIgnoreCaseAndIncludeSoftDeleted(request.getUsername()).isPresent()) {
            System.out.println("HIT HERE");
            System.out.println(request.getUsername());
            System.out.println(userService.findByUsernameIgnoreCaseAndIncludeSoftDeleted(request.getUsername()));
            throw new UsernameAlreadyExistsException("This username is already taken");
        }

        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .build();
        userService.save(user);

        return new RegisterResponse("User registered successfully");
    }

    public LoginResponse login(LoginRequest request) {
        // Throws BadCredentialsException or InternalAuthenticationServiceException if authentication fails
        // (Handled by GlobalExceptionHandler)
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        User user = userService.findByUsernameIgnoreCaseAndIncludeSoftDeleted(request.getUsername())
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        String token = jwtService.generateToken(user);

        return new LoginResponse(token);
    }
}
