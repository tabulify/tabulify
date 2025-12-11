package com.tabulify.type;

import com.tabulify.exception.CastException;

public class EmailCastException extends CastException {
  public EmailCastException(String message) {
    super(message);
  }

  public EmailCastException(Exception e) {
    super(e);
  }
}
