package com.amedvedev.mediaspace.story.service;

import com.amedvedev.mediaspace.feed.StoryFeedRedisService;
import com.amedvedev.mediaspace.story.*;
import com.amedvedev.mediaspace.story.dto.StoriesFeedEntry;
import com.amedvedev.mediaspace.story.dto.StoryDto;
import com.amedvedev.mediaspace.story.dto.StoryPreviewResponse;
import com.amedvedev.mediaspace.story.dto.ViewStoryResponse;
import com.amedvedev.mediaspace.story.exception.StoryNotFoundException;
import com.amedvedev.mediaspace.user.User;
import com.amedvedev.mediaspace.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StoryViewService {

    private final StoryMapper storyMapper;
    private final StoryManagementService storyManagementService;
    private final StoryRepository storyRepository;
    private final StoryRedisService storyRedisService;
    private final StoryFeedRedisService storyFeedRedisService;
    private final UserService userService;

    @Transactional(readOnly = true)
    public Set<StoriesFeedEntry> getStoriesFeed() {
        var user = userService.getCurrentUser();
        log.info("Retrieving stories feed for user: {}", user.getUsername());
        
        var storiesFeedResponsesOptional = storyFeedRedisService.getStoriesFeedByUserId(user.getId());
        if (storiesFeedResponsesOptional.isPresent()) {
            log.debug("Stories feed found in cache for user with id: {}", user.getId());
            return storiesFeedResponsesOptional.get();
        }
        
        log.debug("Stories feed not found in cache for user with id: {}", user.getId());
        return getStoriesFromDb(user);
    }

    private Set<StoriesFeedEntry> getStoriesFromDb(User user) {
        var storyFeedProjections = storyRepository.findStoryFeedByUserId(user.getId());
        var storiesFeedResponses = storyFeedProjections.stream()
                .map(storyMapper::toStoryFeedResponse)
                .collect(Collectors.toSet());
        storyFeedRedisService.cacheStoriesFeedByUserId(user.getId(), storiesFeedResponses);
        return storiesFeedResponses;
    }

    @Transactional(readOnly = true)
    public List<StoryPreviewResponse> getCurrentUserStoriesPreviews() {
        var user = userService.getCurrentUserDto();
        return getStoriesPreviewsOfUser(user.getUsername());
    }

    @Transactional(readOnly = true)
    public List<StoryPreviewResponse> getStoriesPreviewsOfUser(String username) {
        log.info("Retrieving stories of user: {}", username);
        var user = userService.getUserDtoByUsername(username);
        return getStoryPreviewResponses(user.getId());
    }

    private List<StoryPreviewResponse> getStoryPreviewResponses(Long userId) {
        var storiesIds = storyRedisService.getStoriesIdsForUser(userId);

        if (storiesIds.isEmpty()) {
            log.debug("No stories ids found in cache for constructing stories previews for user with id: {}", userId);
            var stories = storyManagementService.getStoriesByUserId(userId);

            if (stories.isEmpty()) {
                log.warn("No stories found for user with id: {}", userId);
                return List.of();
            }

            cacheStoriesIdsAndStories(userId, stories);
            
            return mapStoriesToStoryPreviewResponseList(stories);
        }

        log.debug("Stories ids found in cache, constructing stories feed for user with id: {}", userId);
        return mapStoriesIdsToStoryPreviewResponses(storiesIds);
    }

    private void cacheStoriesIdsAndStories(Long userId, List<Story> stories) {
        var storiesIds = stories.stream().map(Story::getId).toList();
        stories.forEach(story -> storyRedisService.cacheStoryDto(storyMapper.toStoryDto(story)));
        storyRedisService.cacheStoriesIdsForUser(userId, storiesIds);
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

    @Transactional(readOnly = true)
    public ViewStoryResponse getViewStoryResponseByStoryId(Long id) {
        log.info("Getting ViewStoryResponse for story with id: {}", id);
        var storyDtoOptional = getStoryDtoById(id);
        return storyMapper.toViewStoryResponse(storyDtoOptional.orElseThrow(
                () -> new StoryNotFoundException("Story not found")));
    }

    private Optional<StoryDto> getStoryDtoById(Long id) {
        var cachedStory = storyRedisService.getStoryDtoById(id);
        if (cachedStory.isPresent()) {
            log.debug("Story with id {} found in cache", id);
            return cachedStory;
        }
        log.debug("Story with id {} not found in cache", id);
        return findAndCacheStoryDto(id);
    }

    private Optional<StoryDto> findAndCacheStoryDto(Long id) {
        Story story;
        try {
            story = storyManagementService.findStoryById(id);
        } catch (StoryNotFoundException e) {
            log.warn("Story with id {} not found", id);
            return Optional.empty();
        }
        var storyDto = storyMapper.toStoryDto(story);
        storyRedisService.cacheStoryDto(storyDto);
        return Optional.of(storyDto);
    }
}
