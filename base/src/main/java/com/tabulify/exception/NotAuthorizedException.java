package com.tabulify.exception;

/**
 * Fighting against NULL.
 * Uses this instead of returning null
 */
public class NotAuthorizedException extends Exception {

  public NotAuthorizedException() {
  }

  public NotAuthorizedException(String s) {
    super(s);
  }

  public NotAuthorizedException(String message, Exception e) {
    super(message,e);
  }
}
