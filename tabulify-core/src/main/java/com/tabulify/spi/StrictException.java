package com.tabulify.spi;

/**
 * Exception that are thrown when a strict parameter is set to true
 * <p>
 * (ie a user or environment fault, not a tabulify vault)
 * Example:
 * If the execution of a script returns a bad exit value and if the execution is strict, we throw
 */
public class StrictException extends IllegalArgumentException {

  public static final String STRING = "\n\nYou can turn off this error by setting the execution has not strict.";

  /**
   * When we see an inconsistency
   *
   * @param message - the message
   */
  public StrictException(String message) {
    super(createMessage(message, null));
  }

  /**
   * When an error occurs (we can't get meta) but don't know if this is bad
   *
   * @param message - the message
   */
  public StrictException(String message, Throwable e) {
    super(createMessage(message, e), e);
  }

  private static String createMessage(String message, Throwable e) {
    if (e instanceof StrictException) {
      return message;
    }
    return message + STRING;
  }
}
