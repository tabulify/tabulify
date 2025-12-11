package com.tabulify.type;

import com.tabulify.exception.CastException;

/**
 * Scoped cast exception
 * because if there is CastException everywhere
 * we would not see them
 */
public class DnsCastException extends CastException {

  public DnsCastException(Throwable cause) {
    super(cause);
  }


}
