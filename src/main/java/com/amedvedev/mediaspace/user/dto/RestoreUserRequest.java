package com.amedvedev.mediaspace.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RestoreUserRequest {

    @Schema(description = "Username", example = "username")
    private String username;

    @Schema(description = "Password", example = "password")
    private String password;
}