package com.tabulify.exception;

/**
 * Fighting against NULL.
 * Uses this instead of returning null
 */
public class NoColumnException extends Exception {

  public NoColumnException() {
    super();
  }

  public NoColumnException(String s) {
    super(s);
  }

}
