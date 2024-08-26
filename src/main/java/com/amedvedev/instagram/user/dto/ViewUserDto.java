package com.amedvedev.instagram.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ViewUserDto {
    private String username;
    private String profilePictureUrl;
    private List<Long> postsIds;
    private List<Long> storiesIds;
    private long followersCount;
    private long followingCount;
}


