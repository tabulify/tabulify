package com.tabulify.exception;

/**
 * Fighting against NULL.
 * Uses this instead of returning null
 */
public class NoObjectException extends Exception {

  public NoObjectException() {
    super();
  }

  public NoObjectException(String s) {
    super(s);
  }

}
