package com.amedvedev.mediaspace.post.comment.exception;

import com.amedvedev.mediaspace.exception.ElementNotFoundException;

public class CommentNotFoundException extends ElementNotFoundException {
    public CommentNotFoundException(String message) {
        super(message);
    }
}
