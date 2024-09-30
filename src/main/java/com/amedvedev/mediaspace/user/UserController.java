package com.amedvedev.mediaspace.user;

import com.amedvedev.mediaspace.user.dto.*;
import com.amedvedev.mediaspace.exception.dto.GeneralErrorResponse;
import com.amedvedev.mediaspace.exception.dto.ValidationErrorResponse;
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
    private final UserProfileService userProfileService;

    // TODO: REFACTOR INTO SEARCHING MANY USERS
//    @Operation(summary = "Get user by username", description = "Returns the user by username.")
//    @ApiResponses(value = {
//            @ApiResponse(
//                    responseCode = "200", description = "User found",
//                    content = @Content(schema = @Schema(implementation = UserDto.class))
//            ),
//            @ApiResponse(
//                    responseCode = "401", description = "Unauthorized",
//                    content = @Content(schema = @Schema(implementation = GeneralErrorResponse.class))
//            ),
//            @ApiResponse(
//                    responseCode = "404", description = "User not found",
//                    content = @Content(schema = @Schema(implementation = GeneralErrorResponse.class))
//            )
//    })
//    @GetMapping("/{username}")
//    @ResponseStatus(HttpStatus.OK)
//    public UserDto findByUsernameIgnoreCase(@PathVariable String username) {
//        return userService.getUserDtoByUsername(username);
//    }

    @Operation(summary = "Get authenticated user")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200", description = "Authenticated user data",
                    content = @Content(schema = @Schema(implementation = ViewUserProfileResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = GeneralErrorResponse.class))
            ),
    })
    @GetMapping("/me")
    @ResponseStatus(HttpStatus.OK)
    public ViewUserProfileResponse me() {
        return userProfileService.getCurrentUserProfile();
    }

    @Operation(summary = "Get user profile by username")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200", description = "User found",
                    content = @Content(schema = @Schema(implementation = ViewUserProfileResponse.class))
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
    @GetMapping("/{username}")
    @ResponseStatus(HttpStatus.OK)
    public ViewUserProfileResponse getUserProfile(@PathVariable String username) {
        return userProfileService.getUserProfile(username);
    }

    @Operation(summary = "Follow a user")
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
    @PostMapping("/{username}/follow")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void followUser(@PathVariable String username) {
        userService.followUser(username);
    }

    @Operation(summary = "Unfollow a user")
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
    @DeleteMapping("/{username}/follow")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unfollowUser(@PathVariable String username) {
        userService.unfollowUser(username);
    }

    @Operation(summary = "Change username")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200", description = "Username changed successfully",
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
    @PatchMapping("/username")
    @ResponseStatus(HttpStatus.OK)
    public UpdateUserResponse changeUsername(@Valid @RequestBody ChangeUsernameRequest changeUsernameRequest) {
        return userService.changeUsername(changeUsernameRequest);
    }

    @Operation(summary = "Change password")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200", description = "Password changed successfully",
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
    @PatchMapping("/password")
    @ResponseStatus(HttpStatus.OK)
    public UpdateUserResponse changePassword(@Valid @RequestBody ChangePasswordRequest changePasswordRequest) {
        return userService.changePassword(changePasswordRequest);
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


    @Operation(summary = "Restore user")
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
        return userService.restoreUser(request);
    }
}
