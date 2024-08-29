package com.amedvedev.mediaspace.user;

import com.amedvedev.mediaspace.exception.GeneralErrorResponse;
import com.amedvedev.mediaspace.user.dto.UpdateUserDto;
import com.amedvedev.mediaspace.user.dto.ViewUserDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Tag(name = "User", description = "Endpoints for managing users")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "Get authenticated user", description = "Returns main info about the authenticated user.")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200", description = "Authenticated user data",
                    content = @Content(schema = @Schema(implementation = ViewUserDto.class))
            ),
            @ApiResponse(
                    responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = GeneralErrorResponse.class))
            ),
    })
    @GetMapping("/me")
    @ResponseStatus(HttpStatus.OK)
    public ViewUserDto me() {
        return userService.me();
    }

    @Operation(summary = "Update user information", description = "Updates the information of an existing user.")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200", description = "User updated successfully",
                    content = @Content(schema = @Schema(implementation = Map.class))
            ),
            @ApiResponse(
                    responseCode = "400", description = "Invalid input",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "404", description = "User not found",
                    content = @Content
            )
    })
    @PatchMapping("/{username}")
    public ResponseEntity<Map<String, String>> updateUserInfo(@PathVariable String username, @Valid @RequestBody UpdateUserDto updateUserDto) {

        userService.updateUser(username, updateUserDto);
        return ResponseEntity.status(HttpStatus.OK).body(
                new HashMap<>() {{
                    put("message", "Please, log in again with your new credentials");
                }}
        );
    }
}
