package net.bytle.dns;

import org.xbill.DNS.TextParseException;

public class DnsIllegalArgumentException extends Exception {
  public DnsIllegalArgumentException(TextParseException e) {
    super(e);
  }
}
