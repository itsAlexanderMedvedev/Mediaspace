package com.amedvedev.mediaspace.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class UserDto {

    @EqualsAndHashCode.Include
    @Schema(description = "User ID", example = "1")
    private Long id;

    @Schema(description = "Username", example = "username")
    private String username;

    @Schema(description = "Profile picture URL", example = "https://example.com/profile.jpg")
    private String profilePictureUrl;

}


