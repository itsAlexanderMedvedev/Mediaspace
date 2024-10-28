package com.amedvedev.mediaspace.user.service;

import com.amedvedev.mediaspace.post.PostMapper;
import com.amedvedev.mediaspace.post.PostService;
import com.amedvedev.mediaspace.post.dto.UserProfilePostResponse;
import com.amedvedev.mediaspace.story.service.StoryManagementService;
import com.amedvedev.mediaspace.user.UserMapper;
import com.amedvedev.mediaspace.user.dto.UserDto;
import com.amedvedev.mediaspace.user.dto.ViewUserProfileResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final UserService userService;
    private final PostService postService;
    private final StoryManagementService storyManagementService;
    private final UserMapper userMapper;
    private final PostMapper postMapper;

    @Transactional(readOnly = true)
    public ViewUserProfileResponse getCurrentUserProfile() {
        log.info("Fetching current user's profile");
        return getUserProfileForUserDto(userService.getCurrentUserDto());
    }

    @Transactional(readOnly = true)
    public ViewUserProfileResponse getUserProfile(String username) {
        log.info("Fetching profile of user with username: {}", username);
        return getUserProfileForUserDto(userService.getUserDtoByUsername(username));
    }

    // TODO: REFACTOR USING ENTITY GRAPH OR JOIN FETCH
    @Transactional(propagation = Propagation.MANDATORY)
    public ViewUserProfileResponse getUserProfileForUserDto(UserDto userDto) {
        var id = userDto.getId();
        log.info("Fetching profile of user with id: {}", id);
        var posts = getPosts(id);
        var stories = storyManagementService.getStoriesIdsByUserId(id);
        var followersCount = userService.getFollowersCount(id);
        var followingCount = userService.getFollowingCount(id);
        log.debug("Mapping user profile response");
        return userMapper.toViewUserProfileResponse(userDto, posts, stories, followersCount, followingCount);
    }

    private List<UserProfilePostResponse> getPosts(Long id) {
        var posts = postService.findPostsByUserId(id);
        return posts.stream()
                .map(postMapper::toUserProfilePostResponse)
                .toList();
    }
}
