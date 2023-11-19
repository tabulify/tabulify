package net.bytle.exception;

/**
 * Used when an assertion or check fail
 */
public class AssertionException extends Exception {


  public AssertionException(String s) {
    super(s);
  }

  public AssertionException(String s, Exception e) {
    super(s,e);
  }

  public AssertionException() {
    super();
  }
}
