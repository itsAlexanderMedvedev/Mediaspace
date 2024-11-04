package com.amedvedev.mediaspace.story.listener;

import com.amedvedev.mediaspace.story.dto.StoriesFeedEntry;
import com.amedvedev.mediaspace.story.service.StoryRedisService;
import com.amedvedev.mediaspace.story.event.StoryCreatedEvent;
import com.amedvedev.mediaspace.user.User;
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
        log.debug("Received story created event for story with id: {}", story.getId());
        var publisher = story.getUser();
        var publisherId = publisher.getId();
        var followersIds = userService.getFollowersIdsByUserId(publisherId);
        var storiesFeedEntry = StoriesFeedEntry.builder()
                .username(publisher.getUsername())
                .profilePictureUrl(publisher.getProfilePictureUrl())
                .build();
        storyRedisService.addFeedEntryToFollowersFeed(publisherId, storiesFeedEntry, followersIds);
    }
}
