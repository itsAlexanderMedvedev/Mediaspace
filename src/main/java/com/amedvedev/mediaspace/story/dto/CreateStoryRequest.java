package com.amedvedev.mediaspace.story.dto;

import com.amedvedev.mediaspace.media.dto.CreateMediaRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
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

    @Valid
    @NotNull
    @Schema(description = "The media content associated with the story")
    private CreateMediaRequest createMediaRequest;

}
