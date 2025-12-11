package com.tabulify.exception;

/**
 * Throws when a conf is mandatory and was not found
 * Fighting against NULL.
 * Uses this instead of returning null
 */
public class NoConfException extends Exception {

  public NoConfException() {
    super();
  }

  public NoConfException(String s) {
    super(s);
  }

}
