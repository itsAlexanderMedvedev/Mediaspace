package com.amedvedev.mediaspace.story.exception;

import com.amedvedev.mediaspace.exception.ForbiddenActionException;

public class StoriesLimitReachedException extends ForbiddenActionException {
    public StoriesLimitReachedException(String message) {
        super(message);
    }
}
