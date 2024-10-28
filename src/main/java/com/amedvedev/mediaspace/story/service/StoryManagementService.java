package com.amedvedev.mediaspace.story.service;

import com.amedvedev.mediaspace.exception.ForbiddenActionException;
import com.amedvedev.mediaspace.media.Media;
import com.amedvedev.mediaspace.story.Story;
import com.amedvedev.mediaspace.story.StoryMapper;
import com.amedvedev.mediaspace.story.StoryRepository;
import com.amedvedev.mediaspace.story.dto.CreateStoryRequest;
import com.amedvedev.mediaspace.story.dto.StoryDto;
import com.amedvedev.mediaspace.story.event.StoryCreatedEvent;
import com.amedvedev.mediaspace.story.exception.StoriesLimitReachedException;
import com.amedvedev.mediaspace.story.exception.StoryNotFoundException;
import com.amedvedev.mediaspace.user.User;
import com.amedvedev.mediaspace.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class StoryManagementService {

    private final UserService userService;
    private final StoryRepository storyRepository;
    private final StoryMapper storyMapper;
    private final StoryRedisService storyRedisService;
    private final ApplicationEventPublisher eventPublisher;

    private static final int MAXIMUM_STORIES_COUNT = 30;

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

    public Story findStoryById(Long id) {
        log.debug("Retrieving story with id: {}", id);
        return storyRepository.findById(id).orElseThrow(() -> {
            log.warn("Story with id {} not found", id);
            return new StoryNotFoundException("Story not found");
        });
    }

    public List<Story> getStoriesByUserId(Long userId) {
        log.info("Retrieving stories from database of user with id: {}", userId);
        return storyRepository.findByUserId(userId);
    }

    public List<Long> getStoriesIdsByUserId(Long userId) {
        log.info("Retrieving stories ids from database of user with id: {}", userId);
        return storyRepository.findStoriesIdsByUserId(userId);
    }
}