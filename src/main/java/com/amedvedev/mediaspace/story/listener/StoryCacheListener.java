package com.amedvedev.mediaspace.story.listener;

import com.amedvedev.mediaspace.story.StoryMapper;
import com.amedvedev.mediaspace.story.service.StoryRedisService;
import com.amedvedev.mediaspace.story.event.StoryCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class StoryCacheListener {

    private final StoryRedisService storyRedisService;
    private final StoryMapper storyMapper;

    @Async
    @EventListener
    public void onStoryCreated(StoryCreatedEvent event) {
        var story = event.getStory();
        log.debug("Received story created event for story with id: {}", story.getId());
        var storyDto = storyMapper.toStoryDto(story);
        storyRedisService.cacheStoryDto(storyDto);
        storyRedisService.cacheStoryIdToUserStories(story.getUser().getId(), story);
    }
}