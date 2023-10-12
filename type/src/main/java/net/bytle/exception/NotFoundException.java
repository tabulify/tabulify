package net.bytle.exception;

/**
 * Fighting against NULL.
 * Uses this instead of returning null
 */
public class NotFoundException extends Exception {

  public NotFoundException() {
  }

  public NotFoundException(String s) {
    super(s);
  }

  public NotFoundException(String message, Exception e) {
    super(message,e);
  }
}
