package com.amedvedev.mediaspace.post.comment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EditCommentRequest {

    @Schema(description = "The new comment body", example = "This is an updated comment")
    private String updatedBody;
}
