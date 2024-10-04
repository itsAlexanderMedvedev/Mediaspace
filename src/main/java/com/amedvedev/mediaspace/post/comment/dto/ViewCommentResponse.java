package com.amedvedev.mediaspace.post.comment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ViewCommentResponse {

    @Schema(description = "The comment ID", example = "1")
    private Long id;

    @Schema(description = "The comment body", example = "This is a comment")
    private String body;

    @Schema(description = "The comment author", example = "username")
    private String author;

    @Schema(description = "The comment written at")
    private LocalDateTime writtenAt;

    @Schema(description = "The nested comments")
    private List<ViewCommentResponse> nestedComments;
}
