package com.amedvedev.mediaspace.post.exception;

import com.amedvedev.mediaspace.exception.ElementNotFoundException;
import jakarta.persistence.EntityNotFoundException;

public class PostNotFoundException extends ElementNotFoundException {
    public PostNotFoundException(String message) {
        super(message);
    }
}
