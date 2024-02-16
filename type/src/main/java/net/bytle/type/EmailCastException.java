package net.bytle.type;

import net.bytle.exception.CastException;

public class EmailCastException extends CastException {
  public EmailCastException(String message) {
    super(message);
  }
}
