package com.tabulify.exception;

/**
 * Fighting against NULL.
 * Uses this instead of returning null
 */
public class NoPathFoundException extends Throwable {

  public NoPathFoundException(String s) {
    super(s);
  }

}
