package com.amedvedev.mediaspace.story;

import com.amedvedev.mediaspace.exception.ForbiddenActionException;
import com.amedvedev.mediaspace.redis.RedisService;
import com.amedvedev.mediaspace.story.dto.ViewStoriesFeedResponse;
import com.amedvedev.mediaspace.story.exception.StoriesLimitReachedException;
import com.amedvedev.mediaspace.story.exception.StoryNotFoundException;
import com.amedvedev.mediaspace.user.User;
import com.amedvedev.mediaspace.user.UserService;
import com.amedvedev.mediaspace.media.Media;
import com.amedvedev.mediaspace.story.dto.CreateStoryRequest;
import com.amedvedev.mediaspace.story.dto.ViewStoryResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class StoryService {

    private final UserService userService;
    private final StoryRepository storyRepository;
    private final StoryMapper storyMapper;
    private final RedisService redisService;

    public static final int MAXIMUM_STORIES_COUNT = 30;

    @Transactional
    public ViewStoryResponse createStory(CreateStoryRequest request) {
        var user = userService.getCurrentUser();
        log.info("Creating story for user: {}", user.getUsername());

        if (user.getStories().size() == MAXIMUM_STORIES_COUNT) {
            log.warn("Maximum number of stories reached for user: {}", user.getUsername());
            throw new StoriesLimitReachedException("Maximum number of stories reached");
        }

        var savedStory = storyRepository.save(buildStory(request, user));
        return storyMapper.toViewStoryDto(savedStory);
    }

    private static Story buildStory(CreateStoryRequest request, User user) {
        log.debug("Building story from request");

        var media = Media.builder()
                .url(request.getCreateMediaRequest().getUrl())
                .build();

        var story = Story.builder()
                .user(user)
                .media(media)
                .build();

        user.getStories().add(story);

        return story;
    }

    @Transactional(readOnly = true)
    public List<ViewStoryResponse> getStoriesOfUser(String username) {
        var user = userService.getUserDtoByUsername(username);
        log.info("Retrieving stories of user: {}", username);

        var stories = storyRepository.findByUserId(user.getId()).stream()
                .map(storyMapper::toViewStoryDto)
                .toList();

        if (stories.isEmpty()) {
            log.warn("User {} has no stories", username);
            throw new StoryNotFoundException(String.format("User %s has no stories", username));
        }

        return stories;
    }

    public List<Story> getStoriesByUserId(Long userId) {
        log.debug("Retrieving stories of user with id: {}", userId);
        return storyRepository.findByUserId(userId);
    }

    @Transactional(readOnly = true)
    public ViewStoryResponse getViewStoryResponseByStoryId(Long id) {
        log.info("Getting ViewStoryResponse for story with id: {}", id);
        var story = findStoryById(id);
        return storyMapper.toViewStoryDto(story);
    }

    @Transactional
    public void deleteStory(Long id) {
        log.info("Deleting story with id: {}", id);
        var story = findStoryById(id);

        if (belongsToAnotherUser(story)) {
            log.warn("Cannot delete story of another user");
            throw new ForbiddenActionException("Cannot delete story of another user");
        }

        storyRepository.delete(story);
    }

    private static boolean belongsToAnotherUser(Story story) {
        return !story.getUser().getUsername().equals(SecurityContextHolder.getContext().getAuthentication().getName());
    }

    @Transactional(readOnly = true)
    public List<ViewStoriesFeedResponse> getStoriesFeed() {
        var user = userService.getCurrentUser();
        var stories = redisService.getCachedStoriesFeedForUser(user.getId());

//        StoryMapper
        if (stories.isEmpty()) {
            log.debug("No stories feed found in cache for user with id: {}", user.getId());
            log.debug("Retrieving stories feed from database for user with id: {}", user.getId());

            stories = storyRepository.findStoriesFeed(user.getId()).stream()
                    .map(storyMapper::toViewStoriesFeedResponse)
                    .toList();

            redisService.cacheStoriesFeedForUser(user.getId(), stories);
        }

        return stories;
    }

    private Story findStoryById(Long id) {
        log.debug("Retrieving story with id: {}", id);
        return storyRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Story with id {} not found", id);
                    return new StoryNotFoundException("Story not found");
                });
    }
}
