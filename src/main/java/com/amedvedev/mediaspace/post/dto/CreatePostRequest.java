package com.amedvedev.mediaspace.post.dto;

import com.amedvedev.mediaspace.media.dto.CreateMediaRequest;
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
public class CreatePostRequest {

    @Schema(description = "The title of the post", example = "This is a title")
    private String title;

    @Schema(description = "The description of the post", example = "This is a description")
    private String description;

    @Schema(description = "The list of media URLs")
    private List<CreateMediaRequest> mediaUrls;
}
