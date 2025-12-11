package com.tabulify.exception;

/**
 * Unfortunately, {@link UnsupportedOperationException} is runtime only
 */
public class UnsupportedOperationCompileException extends Exception {
  public UnsupportedOperationCompileException(String message) {
    super(message);
  }
}
