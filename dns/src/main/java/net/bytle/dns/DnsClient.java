package net.bytle.dns;

import java.util.List;
import java.util.Set;

/**
 * Dns Client interface
 * * lookup method return the first record
 * * resolve method returns a list
 */
public interface DnsClient {


  /**
   * @param dnsName - the DNS name
   * @return all Txt
   */
  List<String> resolveTxt(DnsName dnsName) throws DnsException, DnsNotFoundException;

  /**
   *
   * @param dnsName - the DNS Name
   * @return the mx records
   */
  List<DnsMxRecord> resolveMx(DnsName dnsName) throws DnsNotFoundException, DnsException;

  /**
   * @param dnsName - the DNS name
   * @return the first A record
   * behaves the same way as you may be used
   * from when using "nslookup" on your operating system.
   */
  @SuppressWarnings("unused")
  DnsIp lookupA(DnsName dnsName);

  /**
   * @param dnsName - the DNS name
   * @return the first AAAA record
   * behaves the same way as you may be used
   * from when using "nslookup" on your operating system.
   */
  @SuppressWarnings("unused")
  DnsIp lookupAAAA(DnsName dnsName);


  String lookupTxt(DnsName dnsName) throws DnsException, DnsNotFoundException;

  Set<DnsIp> resolveAAAA(DnsName dnsName) throws DnsNotFoundException, DnsException;

  DnsName createDnsName(String name) throws DnsIllegalArgumentException;

  Set<DnsIp> resolveA(DnsName dnsName) throws DnsNotFoundException, DnsException;

  Set<DnsName> lookupCName(DnsName dnsName) throws DnsNotFoundException, DnsException;

  DnsName resolvePtr(DnsIp dnsIp) throws DnsNotFoundException, DnsException;

}
