package com.tabulify.exception;

/**
 * Fighting against NULL.
 * Uses this instead of returning null
 */
public class NoParentException extends Exception {

  public NoParentException(String s) {
    super(s);
  }

  public NoParentException() {
    super();
  }
}
