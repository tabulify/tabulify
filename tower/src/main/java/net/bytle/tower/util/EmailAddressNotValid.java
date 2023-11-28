package net.bytle.tower.util;

public class EmailAddressNotValid extends Exception {
  public EmailAddressNotValid(String s, Exception e) {
    super(s, e);
  }

  public EmailAddressNotValid(String s) {
    super(s);
  }
}
