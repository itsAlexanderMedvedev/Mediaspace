package com.amedvedev.mediaspace.story.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ViewStoriesFeedResponse {

    @Schema(description = "The story ID", example = "1")
    private String username;

    @Schema(description = "Media URL", example = "https://www.example.com/media.mp4")
    private String userPicture;

    @Schema(description = "The story ID", example = "1")
    private Long storyId;
}
