package com.tabulify.tabul;

public class IllegalCommandException extends IllegalArgumentException {

  public IllegalCommandException(String s) {
    super(s);
  }

  public IllegalCommandException(String s, Throwable e) {
    super(s, e);
  }

}
