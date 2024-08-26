package com.amedvedev.instagram.auth;

import com.amedvedev.instagram.exception.UsernameAlreadyExistsException;
import com.amedvedev.instagram.user.User;
import com.amedvedev.instagram.user.UserService;
import com.amedvedev.instagram.user.dto.ViewUserDto;
import com.amedvedev.instagram.user.mapper.UserMapperImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserMapperImpl userMapper;

    public void register(RegisterRequest request) {
        if (userService.findByUsernameIgnoreCase(request.getUsername()).isPresent()) {
            throw new UsernameAlreadyExistsException("Username already exists");
        }

        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .build();
        userService.save(user);
    }

    public AuthenticationResponse login(LoginRequest request) {
        // Throws BadCredentialsException or InternalAuthenticationServiceException if authentication fails
        // (Handled by GlobalExceptionHandler)
        var authenticate = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );
        System.out.println(authenticationManager.getClass().getSimpleName());

        System.out.println(authenticate.getAuthorities());
        System.out.println(authenticate.getCredentials());
        System.out.println(authenticate.getPrincipal());
        System.out.println(authenticate.getName());

        User user = userService.findByUsernameIgnoreCase(request.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        String token = jwtService.generateToken(user);

        return new AuthenticationResponse(token);
    }

    public ViewUserDto me() {
        var user = userService.findByUsernameIgnoreCase(
                SecurityContextHolder.getContext().getAuthentication().getName()
        ).orElseThrow(() -> new UsernameNotFoundException("You are not logged in"));

        return userMapper.toViewUserDto(user);
    }
}
