package net.bytle.exception;

/**
 * Fighting against NULL.
 * Uses this instead of returning null
 */
public class NotFoundException extends Throwable {

  public NotFoundException() {
  }

  public NotFoundException(String s) {
    super(s);
  }

}
