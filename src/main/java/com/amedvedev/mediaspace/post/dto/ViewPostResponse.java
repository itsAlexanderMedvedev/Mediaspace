package com.amedvedev.mediaspace.post.dto;

import com.amedvedev.mediaspace.media.dto.ViewPostMediaResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ViewPostResponse {

        private Long id;
        private String username;
        private String title;
        private String description;
        private List<ViewPostMediaResponse> postMediaList;
        private int likes;
        private int commentsCount;
        private LocalDateTime createdAt;
}
