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
public class UpdatePasswordRequest {

    @Schema(description = "The old password for the user", example = "old_password", nullable = true)
    private String oldPassword;

    @Size(min = 6, max = 20, message = "Password must be between 6 and 20 characters")
    @Pattern(regexp = "^\\S*$", message = "Password cannot contain spaces")
    @Schema(description = "The new password for the user", example = "new_password", nullable = true)
    private String password;
}
