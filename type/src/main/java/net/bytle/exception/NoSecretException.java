package net.bytle.exception;

/**
 * Fighting against NULL.
 * Uses this instead of returning null
 */
public class NoSecretException extends Exception {

  public NoSecretException() {
    super();
  }

  public NoSecretException(String s) {
    super(s);
  }

}
