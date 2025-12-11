package com.tabulify.exception;

/**
 * Fighting against NULL.
 * Uses this instead of returning null
 */
public class NoVariableException extends Throwable {

  public NoVariableException() {
  }

  public NoVariableException(String s) {
    super(s);
  }

}
