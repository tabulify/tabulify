package net.bytle.dns;

public class DnsException extends Exception {
  public DnsException(String s) {
    super(s);
  }

  public DnsException(Exception e) {
    super(e);
  }
}
