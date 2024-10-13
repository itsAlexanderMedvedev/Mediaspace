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
public class ViewStoryResponse {

    @Schema(description = "The story title", example = "This is a story")
    private String username;

    @Schema(description = "Media URL", example = "https://www.example.com/image.jpg")
    private String mediaUrl;

    @Schema(description = "The creation date and time of the story", example = "31.12.2024 12:00")
    private String createdAt;
}
