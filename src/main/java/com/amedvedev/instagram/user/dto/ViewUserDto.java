package com.amedvedev.instagram.user.dto;

import java.util.List;

public class ViewUserDto {
    private String profilePictureUrl;
    private String username;
    private List<Long> postsIds;
    private List<Long> storiesIds;
    private long followersCount;
    private long followingCount;
}
