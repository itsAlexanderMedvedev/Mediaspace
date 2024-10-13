package com.amedvedev.mediaspace.post.dto;

import com.amedvedev.mediaspace.media.dto.ViewPostMediaResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ViewPostResponse {

    @Schema(description = "The post ID", example = "1")
    private Long id;

    @Schema(description = "The post author's username", example = "user")
    private String username;

    @Schema(description = "The post title", example = "This is a post")
    private String title;

    @Schema(description = "The post description", example = "This is a post description")
    private String description;

    @Schema(description = "The post media list")
    private List<ViewPostMediaResponse> postMediaList;

    @Schema(description = "The post likes count", example = "50")
    private int likes;

    @Schema(description = "The post comments count", example = "5")
    private int commentsCount;

    @Schema(description = "The post creation date", example = "31.12.2024 12:00")
    private String createdAt;
}
