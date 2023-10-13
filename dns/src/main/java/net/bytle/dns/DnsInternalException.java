package net.bytle.dns;

/**
 * An exception that should not occur
 * because:
 * * we check the data quality or the state before
 * * or we wrote the literal code
 */
public class DnsInternalException extends RuntimeException {
  public DnsInternalException(Exception e) {
    super(e);
  }
}
