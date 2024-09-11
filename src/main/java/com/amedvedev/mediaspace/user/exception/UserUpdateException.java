package com.amedvedev.mediaspace.user.exception;

import com.amedvedev.mediaspace.exception.BadRequestActionException;

public class UserUpdateException extends BadRequestActionException {
  public UserUpdateException(String message) {
    super(message);
  }
}
