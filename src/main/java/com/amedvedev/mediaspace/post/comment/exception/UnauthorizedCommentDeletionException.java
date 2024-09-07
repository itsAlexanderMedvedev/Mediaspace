package com.amedvedev.mediaspace.post.comment.exception;

public class UnauthorizedCommentDeletionException extends RuntimeException {
    public UnauthorizedCommentDeletionException(String message) {
        super(message);
    }
}
