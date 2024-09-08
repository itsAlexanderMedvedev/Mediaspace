package com.amedvedev.mediaspace.post.dto;

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

    // TODO: add open api docs
    private String title;
    private String description;

    // TODO: validate media urls
    private List<String> mediaUrls;
}
