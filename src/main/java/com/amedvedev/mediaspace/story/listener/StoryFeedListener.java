package com.amedvedev.mediaspace.story.listener;

import com.amedvedev.mediaspace.story.service.StoryRedisService;
import com.amedvedev.mediaspace.story.event.StoryCreatedEvent;
import com.amedvedev.mediaspace.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class StoryFeedListener {

    private final UserService userService;
    private final StoryRedisService storyRedisService;

    @Async
    @EventListener
    public void onStoryCreated(StoryCreatedEvent event) {
        var story = event.getStory();
        var publisherId = story.getUser().getId();
        var followersIds = userService.getFollowersIdsByUserId(publisherId);
        storyRedisService.addPublisherIdToFollowersFeed(followersIds, publisherId, story.getCreatedAt());
    }
}
