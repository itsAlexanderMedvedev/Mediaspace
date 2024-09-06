package com.amedvedev.mediaspace.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UpdateUserRequest {

    @Nullable
    @Size(min = 3, max = 20, message = "Username must be between 3 and 20 characters")
    @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "Username must not contain spaces and may only include English letters, digits, underscores (_), hyphens (-), or periods (.)")
    @Schema(description = "The new username for the user", example = "new_username", nullable = true)
    private String username;

    @Nullable
    @Size(min = 6, max = 20, message = "Password must be between 6 and 20 characters")
    @Pattern(regexp = "^\\S*$", message = "Password cannot contain spaces")
    @Schema(description = "The new password for the user", example = "new_password", nullable = true)
    private String password;
}