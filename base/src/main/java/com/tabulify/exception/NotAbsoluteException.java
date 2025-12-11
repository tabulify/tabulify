package com.tabulify.exception;

/**
 * Fighting against NULL.
 * Uses this instead of returning null
 */
public class NotAbsoluteException extends Exception {

  public NotAbsoluteException() {
    super();
  }

  public NotAbsoluteException(String s) {
    super(s);
  }

}
