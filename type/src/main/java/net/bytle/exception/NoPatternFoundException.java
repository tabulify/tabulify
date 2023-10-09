package net.bytle.exception;

/**
 * Fighting against NULL.
 * Uses this instead of returning null
 */
public class NoPatternFoundException extends Throwable {

  public NoPatternFoundException(String s) {
    super(s);
  }

}
