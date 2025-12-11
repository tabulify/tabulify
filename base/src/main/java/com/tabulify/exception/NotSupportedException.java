package com.tabulify.exception;

/**
 * Fighting against NULL.
 * Uses this instead of returning null
 */
public class NotSupportedException extends Exception {

  public NotSupportedException(String s) {
    super(s);
  }

}
