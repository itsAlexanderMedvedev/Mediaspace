package com.amedvedev.instagram.story.dto;

import com.amedvedev.instagram.media.dto.CreateMediaDto;
import com.amedvedev.instagram.user.User;
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
