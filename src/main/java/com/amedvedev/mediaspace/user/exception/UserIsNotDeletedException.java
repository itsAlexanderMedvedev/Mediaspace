package com.amedvedev.mediaspace.user.exception;

import com.amedvedev.mediaspace.exception.BadRequestActionException;

public class UserIsNotDeletedException extends BadRequestActionException {
    public UserIsNotDeletedException(String message) {
        super(message);
    }
}
