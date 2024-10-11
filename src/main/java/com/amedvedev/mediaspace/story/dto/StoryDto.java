package com.amedvedev.mediaspace.story.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoryDto {

    @Schema(description = "The story ID", example = "1")
    private Long id;

    @Schema(description = "The media URL", example = "https://www.example.com/image.jpg")
    private String mediaUrl;

    @Schema(description = "The creation date and time of the story", example = "2021-07-01T12:00:00")
    private Instant createdAt;
}
