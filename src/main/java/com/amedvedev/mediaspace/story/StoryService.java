package com.amedvedev.mediaspace.story;

import com.amedvedev.mediaspace.exception.ForbiddenActionException;
import com.amedvedev.mediaspace.media.Media;
import com.amedvedev.mediaspace.story.dto.CreateStoryRequest;
import com.amedvedev.mediaspace.story.dto.StoryDto;
import com.amedvedev.mediaspace.story.dto.StoryPreviewResponse;
import com.amedvedev.mediaspace.story.dto.ViewStoryResponse;
import com.amedvedev.mediaspace.story.event.StoryCreatedEvent;
import com.amedvedev.mediaspace.story.exception.StoriesLimitReachedException;
import com.amedvedev.mediaspace.story.exception.StoryNotFoundException;
import com.amedvedev.mediaspace.user.User;
import com.amedvedev.mediaspace.user.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class StoryService {

    private final UserService userService;
    private final StoryRepository storyRepository;
    private final StoryMapper storyMapper;
    private final StoryRedisService storyRedisService;
    private final ApplicationEventPublisher eventPublisher;

    public static final int MAXIMUM_STORIES_COUNT = 30;

    @Transactional
    public StoryDto createStory(CreateStoryRequest request) {
        var user = userService.getCurrentUser();
        log.info("Creating story for user: {}", user.getUsername());

        verifyMaximumStoriesCountIsNotReached(user);

        var story = buildStory(request, user);
        var savedStory = storyRepository.save(story);

        eventPublisher.publishEvent(new StoryCreatedEvent(this, savedStory));

        return storyMapper.toStoryDto(savedStory);
    }

    private void verifyMaximumStoriesCountIsNotReached(User user) {
        if (user.getStories().size() == MAXIMUM_STORIES_COUNT) {
            log.warn("Maximum number of stories reached for user: {}", user.getUsername());
            throw new StoriesLimitReachedException("Maximum number of stories reached");
        }
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
    public List<StoryPreviewResponse> getStoryPreviewsOfUser(String username) {
        var user = userService.getUserDtoByUsername(username);
        log.info("Retrieving stories of user: {}", username);

        var stories = getStoryPreviewResponseList(user.getId());
        checkIfUserHasStories(username, stories);

        return stories;
    }

    @Transactional(readOnly = true)
    public List<StoryPreviewResponse> getCurrentUserStories() {
        var user = userService.getCurrentUserDto();
        return getStoryPreviewsOfUser(user.getUsername());
    }

    private void checkIfUserHasStories(String username, List<StoryPreviewResponse> stories) {
        if (stories.isEmpty()) {
            log.warn("User {} has no stories", username);
            throw new StoryNotFoundException(String.format("User %s has no stories", username));
        }
    }

    private List<StoryPreviewResponse> getStoryPreviewResponseList(Long userId) {
        var storiesIds = storyRedisService.getStoriesIdsForUser(userId);
        if (storiesIds.isEmpty()) {
            log.debug("No stories ids found in cache for constructing stories for user with id: {}", userId);
            log.debug("Retrieving stories from database for user with id: {}", userId);
            var stories = getStoriesByUserId(userId);
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

    public List<Story> getStoriesByUserId(Long userId) {
        log.info("Retrieving stories of user with id: {}", userId);
        return storyRepository.findByUserId(userId);
    }

    @Transactional(readOnly = true)
    public ViewStoryResponse getViewStoryResponseByStoryId(Long id) {
        log.info("Getting ViewStoryResponse for story with id: {}", id);
        var storyDtoOptional = getStoryDtoById(id);
        return storyMapper.toViewStoryResponse(storyDtoOptional.orElseThrow(
                () -> new StoryNotFoundException("Story not found")));
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
        storyRedisService.removeStoryDtoById(id);
    }

    private static boolean belongsToAnotherUser(Story story) {
        return !story.getUser().getUsername().equals(SecurityContextHolder.getContext().getAuthentication().getName());
    }

    @Transactional(readOnly = true)
    public List<StoryPreviewResponse> getStoriesFeed() {
        var user = userService.getCurrentUser();
        var storiesIds = storyRedisService.getFeedStoriesIdForUser(user.getId());

        if (storiesIds.isEmpty()) {
            log.debug("No stories ids found in cache for constructing stories feed for user with id: {}", user.getId());
            log.debug("Retrieving stories feed from database for user with id: {}", user.getId());
            var stories = getStoriesFeed(user);
            storyRedisService.cacheFeedStoriesIdsForUser(user.getId(), stories);
            return mapStoriesToStoryPreviewResponseList(stories);
        }

        log.debug("Constructing stories feed for user with id: {}", user.getId());
        return mapStoriesIdsToStoryPreviewResponses(storiesIds);
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
        return cachedStory != null ? Optional.of(cachedStory) : findAndCacheStoryDto(id);
    }

    private Optional<StoryDto> findAndCacheStoryDto(Long id) {
        Story story;
        try {
            story = findStoryById(id);
        } catch (StoryNotFoundException e) {
            return Optional.empty();
        }
        var storyDto = storyMapper.toStoryDto(story);
        storyRedisService.cacheStoryDto(id, storyDto);
        return Optional.of(storyMapper.toStoryDto(story));
    }

    private List<Story> getStoriesFeed(User user) {
        return storyRepository.findStoriesFeed(user.getId());
    }

    private Story findStoryById(Long id) {
        log.debug("Retrieving story with id: {}", id);
        return storyRepository.findById(id).orElseThrow(() -> {
            log.warn("Story with id {} not found", id);
            return new StoryNotFoundException("Story not found");
        });
    }
}