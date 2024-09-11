package com.amedvedev.mediaspace.user;

import com.amedvedev.mediaspace.user.dto.RestoreUserRequest;
import com.amedvedev.mediaspace.user.dto.RestoreUserResponse;
import com.amedvedev.mediaspace.exception.dto.GeneralErrorResponse;
import com.amedvedev.mediaspace.exception.dto.ValidationErrorResponse;
import com.amedvedev.mediaspace.user.dto.UpdateUserRequest;
import com.amedvedev.mediaspace.user.dto.UpdateUserResponse;
import com.amedvedev.mediaspace.user.dto.ViewUserResponse;
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

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
@Tag(name = "User", description = "Endpoints for managing users")
public class UserController {

    private final UserService userService;

    @Operation(summary = "Follow a user", description = "Allows the authenticated user to follow another user.")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200", description = "User followed successfully",
                    content = @Content(schema = @Schema(implementation = GeneralErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = GeneralErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404", description = "User not found",
                    content = @Content(schema = @Schema(implementation = GeneralErrorResponse.class))
            )
    })
    @PostMapping("{username}/follow")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void followUser(@PathVariable String username) {
        userService.followUser(username);
    }

    @Operation(summary = "Unfollow a user", description = "Allows the authenticated user to unfollow another user.")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200", description = "User unfollowed successfully",
                    content = @Content(schema = @Schema(implementation = GeneralErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = GeneralErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404", description = "User not found",
                    content = @Content(schema = @Schema(implementation = GeneralErrorResponse.class))
            )
    })
    @DeleteMapping("{username}/follow")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unfollowUser(@PathVariable String username) {
        userService.unfollowUser(username);
    }

    @Operation(summary = "Get authenticated user", description = "Returns main info about the authenticated user.")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200", description = "Authenticated user data",
                    content = @Content(schema = @Schema(implementation = ViewUserResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = GeneralErrorResponse.class))
            ),
    })
    @GetMapping("/me")
    @ResponseStatus(HttpStatus.OK)
    public ViewUserResponse me() {
        return userService.me();
    }


    @Operation(summary = "Update user information", description = "Updates the information of an existing user.")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200", description = "User updated successfully",
                    content = @Content(schema = @Schema(implementation = UpdateUserResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400", description = "Invalid input",
                    content = @Content(schema = @Schema(implementation = ValidationErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = GeneralErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404", description = "User not found",
                    content = @Content(schema = @Schema(implementation = GeneralErrorResponse.class))
            )
    })
    @PatchMapping
    @ResponseStatus(HttpStatus.OK)
    public UpdateUserResponse updateUserInfo(@Valid @RequestBody UpdateUserRequest updateUserRequest) {
        return userService.updateUser(updateUserRequest);
    }

    @Operation(summary = "Delete user", description = "Deletes the authenticated user.")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "204", description = "User deleted successfully"
            ),
            @ApiResponse(
                    responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = GeneralErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404", description = "User not found",
                    content = @Content(schema = @Schema(implementation = GeneralErrorResponse.class))
            )
    })
    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser() {
        userService.deleteUser();
    }


    @Operation(summary = "Restore user", description = "Restores the user that was previously deleted.")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200", description = "User restored successfully",
                    content = @Content(schema = @Schema(implementation = RestoreUserResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = GeneralErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404", description = "User not found",
                    content = @Content(schema = @Schema(implementation = GeneralErrorResponse.class))
            )
    })
    @PutMapping("/restore")
    @ResponseStatus(HttpStatus.OK)
    public RestoreUserResponse restoreUser(@RequestBody RestoreUserRequest request) {
        userService.restoreUser(request);
        return new RestoreUserResponse("User restored successfully. Please login to continue.");
    }
}
