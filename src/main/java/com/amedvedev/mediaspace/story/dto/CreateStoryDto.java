package com.amedvedev.mediaspace.story.dto;

import com.amedvedev.mediaspace.media.dto.CreateMediaDto;
import com.amedvedev.mediaspace.user.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateStoryDto {

    private User user;
    private CreateMediaDto media;

}
