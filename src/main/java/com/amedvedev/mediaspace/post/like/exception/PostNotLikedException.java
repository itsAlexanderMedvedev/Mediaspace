package com.amedvedev.mediaspace.post.like.exception;

import com.amedvedev.mediaspace.exception.BadRequestActionException;

public class PostNotLikedException extends BadRequestActionException {
    public PostNotLikedException(String message) {
        super(message);
    }
}
