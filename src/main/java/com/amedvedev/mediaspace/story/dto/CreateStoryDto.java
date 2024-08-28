package com.amedvedev.mediaspace.story.dto;

import com.amedvedev.mediaspace.media.dto.CreateMediaDto;
import com.amedvedev.mediaspace.user.User;
import io.swagger.v3.oas.annotations.media.Schema;
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
    private Long userId;

    @Schema(description = "The media content associated with the story")
    private CreateMediaDto media;

}
