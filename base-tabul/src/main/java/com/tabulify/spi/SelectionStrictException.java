package com.tabulify.spi;

/**
 * Exception that are thrown when a strict parameter is set to true
 * <p>
 * (ie a user or environment fault, not a tabulify vault)
 * Example:
 * If the execution of a script returns a bad exit value and if the execution is strict, we throw
 */
public class SelectionStrictException extends StrictException {

  public static final String STRING = "\n\nYou can turn off this error by setting the selection has not strict (--no-strict-selection flag for tabul).";

  /**
   * When we see an inconsistency
   *
   * @param message - the message
   */
  public SelectionStrictException(String message) {
    super(createMessage(message, null));
  }

  /**
   * When an error occurs (we can't get meta) but don't know if this is bad
   *
   * @param message - the message
   */
  public SelectionStrictException(String message, Throwable e) {
    super(createMessage(message, e), e);
  }

  private static String createMessage(String message, Throwable e) {
    if (e instanceof SelectionStrictException) {
      return message;
    }
    return message + STRING;
  }
}
