package com.amedvedev.mediaspace.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
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
public class ChangeUsernameRequest {

    @Size(min = 3, max = 20, message = "Username must be between 3 and 20 characters")
    @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "Username must not contain spaces and may only include English letters, digits, underscores (_), hyphens (-), or periods (.)")
    @Schema(description = "The new username for the user", example = "new_username", nullable = true)
    private String username;
}
