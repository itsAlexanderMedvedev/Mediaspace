package com.amedvedev.mediaspace.post.comment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EditCommentRequest {

    @NotBlank
    @Length(max = 2048)
    @Schema(description = "The new comment body", example = "This is an updated comment")
    private String updatedBody;
}
