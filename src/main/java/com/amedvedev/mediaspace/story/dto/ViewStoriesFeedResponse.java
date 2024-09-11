package com.amedvedev.mediaspace.story.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ViewStoriesFeedResponse {

    private String username;
    private String userPicture;
    private Long storyId;
}
