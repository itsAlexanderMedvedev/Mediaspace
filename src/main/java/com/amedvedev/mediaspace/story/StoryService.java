package com.amedvedev.mediaspace.story;

import com.amedvedev.mediaspace.redis.RedisService;
import com.amedvedev.mediaspace.story.dto.ViewStoriesFeedResponse;
import com.amedvedev.mediaspace.story.exception.StoriesLimitReachedException;
import com.amedvedev.mediaspace.story.exception.StoryNotFoundException;
import com.amedvedev.mediaspace.story.exception.ForbiddenActionException;
import com.amedvedev.mediaspace.user.exception.UserNotFoundException;
import com.amedvedev.mediaspace.media.Media;
import com.amedvedev.mediaspace.story.dto.CreateStoryRequest;
import com.amedvedev.mediaspace.story.dto.ViewStoryResponse;
import com.amedvedev.mediaspace.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class StoryService {

    private final UserRepository userRepository;
    private final StoryRepository storyRepository;
    private final StoryMapper storyMapper;
    private final RedisService redisService;

    public ViewStoryResponse createStory(CreateStoryRequest request) {
        var user = userRepository.findByUsernameIgnoreCase(
                SecurityContextHolder.getContext().getAuthentication().getName()
        ).orElseThrow(() -> new UserNotFoundException("Authentication object is invalid or does not contain a username"));

        if (user.getStories().size() == 30) {
            throw new StoriesLimitReachedException("Maximum number of stories reached");
        }

        var media = Media.builder()
                .url(request.getCreateMediaRequest().getUrl())
                .build();

        var story = Story.builder()
                .user(user)
                .media(media)
                .build();

        user.getStories().add(story);

        userRepository.save(user);
        story.setId(user.getStories().getFirst().getId());

        return storyMapper.toViewStoryDto(story);
    }

    public List<ViewStoryResponse> getStoriesOfUser(String username) {
        var user = userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        var stories = storyRepository.findByUserId(user.getId()).stream()
                .map(storyMapper::toViewStoryDto)
                .toList();

        if (stories.isEmpty()) {
            throw new StoryNotFoundException(String.format("User %s has no stories", username));
        }

        return stories;
    }

    public List<Story> getStoriesByUserId(Long userId) {
        return storyRepository.findByUserId(userId);
    }

    public ViewStoryResponse getStoryById(Long id) {
        var story = storyRepository.findById(id)
                .orElseThrow(() -> new StoryNotFoundException("Story not found"));

        return storyMapper.toViewStoryDto(story);
    }

    public void deleteStory(Long id) {
        var story = storyRepository.findById(id)
                .orElseThrow(() -> new StoryNotFoundException("Story not found"));

        if (!story.getUser().getUsername().equals(SecurityContextHolder.getContext().getAuthentication().getName())) {
            throw new ForbiddenActionException("Cannot delete story of another user");
        }

        storyRepository.delete(story);
    }

    public List<ViewStoriesFeedResponse> getStoriesFeed() {
        var user = userRepository.findByUsernameIgnoreCase(
                SecurityContextHolder.getContext().getAuthentication().getName()
        ).orElseThrow(() -> new UserNotFoundException("Authentication object is invalid or does not contain a username"));

        List<ViewStoriesFeedResponse> stories = redisService.getCachedStoriesFeedForUser(user.getId());

        if (stories.isEmpty()) {
            log.info("No stories feed found in cache for user with id: {}", user.getId());
            log.info("Retrieving stories feed from database for user with id: {}", user.getId());
            stories = storyRepository.findStoriesFeed(user.getId()).stream()
                    .map(story -> ViewStoriesFeedResponse.builder()
                            .username(user.getUsername())
                            .userPicture(user.getProfilePicture() == null ? null : user.getProfilePicture().getUrl())
                            .storyId(story.getId())
                            .build()
                    )
                    .toList();

            redisService.cacheStoriesFeedForUser(user.getId(), stories);
        }

        return stories;
    }
}
