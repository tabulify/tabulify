package net.bytle.exception;

/**
 * Fighting against NULL.
 * Uses this instead of returning null
 */
public class NoValueException extends Throwable {

  public NoValueException() {
  }

  public NoValueException(String s) {
    super(s);
  }

}
