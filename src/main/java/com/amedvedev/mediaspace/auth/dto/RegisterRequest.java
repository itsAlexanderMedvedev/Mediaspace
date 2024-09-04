package com.amedvedev.mediaspace.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RegisterRequest {

    @Size(min = 3, max = 20, message = "Username must be between 3 and 20 characters")
    @Pattern(regexp = "^[a-zA-Z0-9._-]*$", message = "Username must not contain spaces and may only include English letters, digits, underscores (_), hyphens (-), or periods (.)")
    @Schema(description = "Username must be between 3 and 20 characters and may only include English letters, digits, underscores (_), hyphens (-), or periods (.)", example = "username")
    private String username;

    @Size(min = 6, max = 20, message = "Password must be between 6 and 20 characters")
    @Pattern(regexp = "^\\S*$", message = "Password cannot contain spaces")
    @Schema(description = "Password must be between 6 and 20 characters and cannot contain spaces", example = "password")
    private String password;
}
