package com.amedvedev.mediaspace.story.service;

import com.amedvedev.mediaspace.story.*;
import com.amedvedev.mediaspace.story.dto.StoryDto;
import com.amedvedev.mediaspace.story.dto.StoryPreviewResponse;
import com.amedvedev.mediaspace.story.dto.ViewStoryResponse;
import com.amedvedev.mediaspace.story.exception.StoryNotFoundException;
import com.amedvedev.mediaspace.user.User;
import com.amedvedev.mediaspace.user.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class StoryViewService {

    private final StoryMapper storyMapper;
    private final StoryManagingService storyManagingService;
    private final StoryRepository storyRepository;
    private final StoryRedisService storyRedisService;
    private final UserService userService;

    @Transactional(readOnly = true)
    public List<StoryPreviewResponse> getStoriesFeed() {
        var user = userService.getCurrentUser();
        var storiesIds = storyRedisService.getFeedStoriesIdForUser(user.getId());

        if (storiesIds.isEmpty()) {
            log.debug("No stories ids found in cache for constructing stories feed for user with id: {}", user.getId());
            var stories = getStoriesFeed(user);
            storyRedisService.cacheFeedStoriesIdsForUser(user.getId(), stories);
            return mapStoriesToStoryPreviewResponseList(stories);
        }

        log.debug("Constructing stories feed for user with id: {}", user.getId());
        return mapStoriesIdsToStoryPreviewResponses(storiesIds);
    }

    private List<Story> getStoriesFeed(User user) {
        log.debug("Retrieving stories feed from database for user with id: {}", user.getId());
        return storyRepository.findStoriesFeed(user.getId());
    }

    @Transactional(readOnly = true)
    public List<StoryPreviewResponse> getStoryPreviewsOfUser(String username) {
        var user = userService.getUserDtoByUsername(username);
        log.info("Retrieving stories of user: {}", username);

        var stories = getStoryPreviewResponseList(user.getId());
        checkIfUserHasStories(username, stories);

        return stories;
    }

    private void checkIfUserHasStories(String username, List<StoryPreviewResponse> stories) {
        if (stories.isEmpty()) {
            log.warn("User {} has no stories", username);
            throw new StoryNotFoundException(String.format("User %s has no stories", username));
        }
    }

    @Transactional(readOnly = true)
    public List<StoryPreviewResponse> getCurrentUserStories() {
        var user = userService.getCurrentUserDto();
        return getStoryPreviewsOfUser(user.getUsername());
    }

    @Transactional(readOnly = true)
    public ViewStoryResponse getViewStoryResponseByStoryId(Long id) {
        log.info("Getting ViewStoryResponse for story with id: {}", id);
        var storyDtoOptional = getStoryDtoById(id);
        return storyMapper.toViewStoryResponse(storyDtoOptional.orElseThrow(
                () -> new StoryNotFoundException("Story not found")));
    }

    private List<StoryPreviewResponse> getStoryPreviewResponseList(Long userId) {
        var storiesIds = storyRedisService.getStoriesIdsForUser(userId);

        if (storiesIds.isEmpty()) {
            log.debug("No stories ids found in cache for constructing stories for user with id: {}", userId);
            var stories = storyManagingService.getStoriesByUserId(userId);

            if (stories.isEmpty()) {
                log.warn("No stories found for user with id: {}", userId);
                return List.of();
            }

            storyRedisService.cacheStoriesIdsForUser(userId, stories);
            return mapStoriesToStoryPreviewResponseList(stories);
        }

        log.debug("Constructing stories for user with id: {}", userId);
        return mapStoriesIdsToStoryPreviewResponses(storiesIds);
    }

    private List<StoryPreviewResponse> mapStoriesToStoryPreviewResponseList(List<Story> stories) {
        return stories.stream().map(storyMapper::toStoryPreviewResponse).toList();
    }

    private List<StoryPreviewResponse> mapStoriesIdsToStoryPreviewResponses(List<Long> storiesIds) {
        return storiesIds.stream()
                .map(this::getStoryDtoById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(storyMapper::toStoryPreviewResponse)
                .toList();
    }

    private Optional<StoryDto> getStoryDtoById(Long id) {
        var cachedStory = storyRedisService.getStoryDtoById(id);
        return cachedStory.isPresent() ? cachedStory : findAndCacheStoryDto(id);
    }

    private Optional<StoryDto> findAndCacheStoryDto(Long id) {
        Story story;
        try {
            story = storyManagingService.findStoryById(id);
        } catch (StoryNotFoundException e) {
            return Optional.empty();
        }
        var storyDto = storyMapper.toStoryDto(story);
        storyRedisService.cacheStoryDto(storyDto);
        return Optional.of(storyMapper.toStoryDto(story));
    }
}
