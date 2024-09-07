package com.amedvedev.mediaspace.story.exception;

import com.amedvedev.mediaspace.exception.ElementNotFoundException;

public class StoryNotFoundException extends ElementNotFoundException {
    public StoryNotFoundException(String message) {
        super(message);
    }
}
