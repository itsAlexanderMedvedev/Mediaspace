package com.amedvedev.mediaspace.story.dto;

import com.amedvedev.mediaspace.media.dto.CreateMediaRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateStoryRequest {

    @NotNull
    @NotEmpty
    @Schema(description = "The username of the user creating the story", example = "username")
    private String username;

    @Schema(description = "The media content associated with the story")
    @NotNull
    private CreateMediaRequest media;

}
