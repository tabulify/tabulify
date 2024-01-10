package net.bytle.type.time;

public class TimeException extends Exception {

  public TimeException(String s, Exception e) {
    super(s, e);
  }

  public TimeException(String s) {
    super(s);
  }
}
