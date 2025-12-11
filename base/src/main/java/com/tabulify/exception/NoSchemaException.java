package com.tabulify.exception;

/**
 * Fighting against NULL.
 * Uses this instead of returning null
 */
public class NoSchemaException extends Exception {

  public NoSchemaException() {
    super();
  }

  public NoSchemaException(String s) {
    super(s);
  }

}
