package com.amedvedev.mediaspace.user.exception;

public class UserIsNotDeletedException extends RuntimeException {
    public UserIsNotDeletedException(String message) {
        super(message);
    }
}
