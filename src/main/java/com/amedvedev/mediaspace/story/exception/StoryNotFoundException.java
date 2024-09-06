package com.amedvedev.mediaspace.story.exception;

public class StoryNotFoundException extends RuntimeException {
    public StoryNotFoundException(String message) {
        super(message);
    }
}
