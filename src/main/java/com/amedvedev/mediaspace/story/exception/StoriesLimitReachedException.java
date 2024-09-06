package com.amedvedev.mediaspace.story.exception;

public class StoriesLimitReachedException extends RuntimeException {
    public StoriesLimitReachedException(String message) {
        super(message);
    }
}
