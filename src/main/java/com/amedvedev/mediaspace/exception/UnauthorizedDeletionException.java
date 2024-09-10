package com.amedvedev.mediaspace.exception;

public class UnauthorizedDeletionException extends RuntimeException {
    public UnauthorizedDeletionException(String message) {
        super(message);
    }
}
