package com.amedvedev.mediaspace.post.dto;

import com.amedvedev.mediaspace.media.dto.CreateMediaRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePostRequest {

    @NotBlank
    @Length(max = 64)
    @Schema(description = "The title of the post", example = "This is a title")
    private String title;

    @Length(max = 2048)
    @Pattern(regexp = "^$|\\S.*", message = "Description cannot be only of whitespaces")
    @Schema(description = "The description of the post", example = "This is a description")
    private String description;

    @Schema(description = "The list of media URLs")
    private List<CreateMediaRequest> mediaUrls;
}
