package com.amedvedev.instagram.auth;

import com.amedvedev.instagram.user.dto.ViewUserDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@Valid @RequestBody RegisterRequest request) {
        authenticationService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(new HashMap<>() {{
            put("message", "User registered successfully");
        }});
    }

    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponse> login(@RequestBody LoginRequest request) {
        var authenticationResponse = authenticationService.login(request);
        return ResponseEntity.ok(authenticationResponse);
    }

    @GetMapping("/me")
    public ResponseEntity<ViewUserDto> me() {
        var authenticationResponse = authenticationService.me();
        return ResponseEntity.ok(authenticationResponse);
    }
}
