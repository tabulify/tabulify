package net.bytle.dns;

public class DnsException extends Exception {

  public DnsException(Exception e) {
    super(e);
  }

  public DnsException(String message, Exception e) {
    super(message,e);
  }
}
