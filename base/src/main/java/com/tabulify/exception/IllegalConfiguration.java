package com.tabulify.exception;

public class IllegalConfiguration extends Exception {


  public IllegalConfiguration(String s) {
    super(s);
  }

  public IllegalConfiguration(String s, Exception e) {
    super(s,e);
  }

}
