package com.amedvedev.mediaspace.user;

import com.amedvedev.mediaspace.post.PostMapper;
import com.amedvedev.mediaspace.post.PostService;
import com.amedvedev.mediaspace.post.dto.UserProfilePostResponse;
import com.amedvedev.mediaspace.story.Story;
import com.amedvedev.mediaspace.story.StoryService;
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
    private final StoryService storyService;
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PostMapper postMapper;
    private final UserRedisService userRedisService;

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
        var posts = getPosts(userDto.getId());
        var stories = getStoriesIds(userDto.getId());
        var followersCount = userService.getFollowersCount(userDto.getId());
        var followingCount = userService.getFollowingCount(userDto.getId());
        log.debug("Fetching profile of user from dto with username: {}", userDto.getUsername());
        return userMapper.toViewUserProfileResponse(userDto, posts, stories, followersCount, followingCount);
    }

    private List<UserProfilePostResponse> getPosts(Long id) {
        var posts = postService.findPostsByUserId(id);
        return posts.stream()
                .map(postMapper::toUserProfilePostResponse)
                .toList();
    }

    private List<Long> getStoriesIds(Long id) {
        return storyService.getStoriesByUserId(id).stream()
                .map(Story::getId)
                .toList();
    }
}
