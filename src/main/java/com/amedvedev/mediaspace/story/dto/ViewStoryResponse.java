package com.amedvedev.mediaspace.story.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ViewStoryResponse {

    private Long id;
    private String username;
    private String mediaUrl;
}
