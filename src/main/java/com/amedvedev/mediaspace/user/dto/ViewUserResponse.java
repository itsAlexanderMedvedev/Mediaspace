package com.amedvedev.mediaspace.user.dto;

import com.amedvedev.mediaspace.post.dto.UserProfilePostResponse;
import com.amedvedev.mediaspace.post.dto.ViewPostResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ViewUserResponse {

    private Long id;
    private String username;
    private String profilePictureUrl;
    private List<UserProfilePostResponse> posts;
    private List<Long> storiesIds;
    private long followersCount;
    private long followingCount;
}


