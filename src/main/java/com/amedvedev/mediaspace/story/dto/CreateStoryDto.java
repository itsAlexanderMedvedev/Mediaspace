package com.amedvedev.mediaspace.story.dto;

import com.amedvedev.mediaspace.media.dto.CreateMediaDto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateStoryDto {

    @Schema(description = "The ID of the user creating the story", example = "1")
    @NotNull
    @Min(1)
    private Long userId;

    @Schema(description = "The media content associated with the story")
    @NotNull
    private CreateMediaDto media;

}
