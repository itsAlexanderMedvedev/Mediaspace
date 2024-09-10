package com.amedvedev.mediaspace.post.comment.exception;

import com.amedvedev.mediaspace.exception.UnauthorizedDeletionException;

public class UnauthorizedCommentDeletionException extends UnauthorizedDeletionException {
    public UnauthorizedCommentDeletionException(String message) {
        super(message);
    }
}
