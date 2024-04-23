package net.bytle.type;

import net.bytle.exception.CastException;

/**
 *
 */
public class HandleCastException extends CastException {



  public HandleCastException(String message, Throwable cause) {
    super(message, cause);
  }


  public HandleCastException(String message) {
    super(message);
  }
}
