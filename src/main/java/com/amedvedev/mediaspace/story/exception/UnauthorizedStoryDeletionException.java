package com.amedvedev.mediaspace.story.exception;

import com.amedvedev.mediaspace.exception.UnauthorizedDeletionException;

public class UnauthorizedStoryDeletionException extends UnauthorizedDeletionException {
    public UnauthorizedStoryDeletionException(String message) {
        super(message);
    }
}
