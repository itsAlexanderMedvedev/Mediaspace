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

    @Schema(description = "The story ID", example = "1")
    private Long id;

    @Schema(description = "The story title", example = "This is a story")
    private String username;

    @Schema(description = "Media URL", example = "https://www.example.com/media.mp4")
    private String mediaUrl;
}
