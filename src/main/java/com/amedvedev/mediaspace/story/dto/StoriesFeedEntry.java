package com.amedvedev.mediaspace.story.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class StoriesFeedEntry {

    // TODO: handle how changing username affects feed
    @EqualsAndHashCode.Include
    @Schema(description = "Username of the story publisher", example = "username")
    private String username;
    
    @Schema(description = "URL of the story publisher's profile picture", example = "https://example.com/profile.jpg")
    private String profilePictureUrl;
}
