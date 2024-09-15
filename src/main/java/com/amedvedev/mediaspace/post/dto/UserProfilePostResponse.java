package com.amedvedev.mediaspace.post.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfilePostResponse {

    @Schema(description = "The post ID", example = "1")
    private Long id;

    @Schema(description = "The post title", example = "This is a post")
    private String title;

    @Schema(description = "Cover image url", example = "https://www.example.com/cover.jpg")
    private String coverImage;
}
