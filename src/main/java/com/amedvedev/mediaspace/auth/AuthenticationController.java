package com.amedvedev.mediaspace.auth;

import com.amedvedev.mediaspace.auth.dto.*;
import com.amedvedev.mediaspace.exception.dto.GeneralErrorResponse;
import com.amedvedev.mediaspace.exception.dto.ValidationErrorResponse;
import com.amedvedev.mediaspace.user.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Authentication", description = "Endpoints for user authentication")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthenticationController {

    private final AuthenticationService authenticationService;
    private final UserService userService;

    @Operation(summary = "Register a new user", description = "Creates a new user account.")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201", description = "User registered successfully",
                    content = @Content(schema = @Schema(implementation = RegisterResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400", description = "Invalid input",
                    content = @Content(schema = @Schema(implementation = ValidationErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "409", description = "This username is already taken",
                    content = @Content(schema = @Schema(implementation = GeneralErrorResponse.class))
            ),
    })
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public RegisterResponse register(@Valid @RequestBody RegisterRequest request) {
        return authenticationService.register(request);
    }


    @Operation(summary = "Login a user", description = "Authenticates a user and returns a JWT token.")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200", description = "Login successful",
                    content = @Content(schema = @Schema(implementation = LoginResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = GeneralErrorResponse.class))
            ),
    })
    @PostMapping("/login")
    @ResponseStatus(HttpStatus.OK)
    public LoginResponse login(@RequestBody LoginRequest request) {
        return authenticationService.login(request);
    }

    @PutMapping("/restore")
    @ResponseStatus(HttpStatus.OK)
    public RestoreResponse restoreUser(@RequestBody RestoreRequest request) {
        userService.restoreUser(request);
        return new RestoreResponse("User restored successfully. Please login to continue.");
    }

}
