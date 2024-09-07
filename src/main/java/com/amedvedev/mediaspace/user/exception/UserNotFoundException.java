package com.amedvedev.mediaspace.user.exception;

import com.amedvedev.mediaspace.exception.ElementNotFoundException;

public class UserNotFoundException extends ElementNotFoundException {
    public UserNotFoundException(String message) {
        super(message);
    }
}
