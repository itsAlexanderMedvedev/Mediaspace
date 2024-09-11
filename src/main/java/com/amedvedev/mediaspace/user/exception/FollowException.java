package com.amedvedev.mediaspace.user.exception;

import com.amedvedev.mediaspace.exception.BadRequestActionException;

public class FollowException extends BadRequestActionException {
    public FollowException(String message) {
        super(message);
    }
}
