package com.amedvedev.mediaspace.exception;

public class BadRequestActionException extends RuntimeException {
    public BadRequestActionException(String message) {
        super(message);
    }
}
