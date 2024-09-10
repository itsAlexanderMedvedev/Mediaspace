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
public class ViewPostCommentsResponse {

    @Schema(description = "The post ID", example = "1")
    private Long postId;

    @Schema(description = "The post comments")
    private List<CommentDto> comments;
}
