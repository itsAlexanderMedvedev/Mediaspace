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
public class StoryPreviewResponse {

    @Schema(description = "The story ID", example = "1")
    private Long storyId;

    @Schema(description = "The publisher username", example = "username")
    private String username;
}
