package com.amedvedev.mediaspace.story.event;

import com.amedvedev.mediaspace.story.Story;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class StoryCreatedEvent extends ApplicationEvent {

    private final Story story;

    public StoryCreatedEvent(Object source, Story story) {
        super(source);
        this.story = story;
    }
}
