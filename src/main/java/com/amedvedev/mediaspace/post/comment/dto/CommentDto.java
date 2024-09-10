package com.amedvedev.mediaspace.post.comment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentDto {

    @Schema(description = "The comment ID", example = "1")
    private Long id;

    @Schema(description = "The comment body", example = "This is a comment")
    private String body;

    @Schema(description = "The comment author", example = "SomeUser")
    private String author;

    @Schema(description = "The comment written at")
    private String writtenAt;

    @Schema(description = "The nested comments")
    private List<CommentDto> nestedComments;
}
