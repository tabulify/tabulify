package net.bytle.dns;

import net.bytle.exception.NotFoundException;
import org.xbill.DNS.*;

import java.net.InetAddress;
import java.util.concurrent.ExecutionException;

public class DnsIp {


  private final InetAddress address;
  private final DnsSession dnsSession;

  public DnsIp(DnsSession session, InetAddress inetAddress) {

    this.dnsSession = session;

    this.address = inetAddress;

  }

  public static DnsIp createFromInetAddress(DnsSession dnsSession, InetAddress inetAddress) {
    return new DnsIp(dnsSession, inetAddress);
  }


  public InetAddress getInetAddress() {
    return this.address;
  }

  @Override
  public String toString() {
    return address.toString();
  }


  public PTRRecord getPtrRecord() throws NotFoundException {


    Name name = ReverseMap.fromAddress(this.address);
    try {
      return dnsSession
        .getLookupSession()
        .lookupAsync(name, Type.PTR)
        .toCompletableFuture()
        .get()
        .getRecords()
        .stream()
        .map(PTRRecord.class::cast)
        .findFirst()
        .orElseThrow(NotFoundException::new);
    } catch (InterruptedException | ExecutionException e) {
      throw new NotFoundException("Network error", e);
    }

  }

  @SuppressWarnings("unused")
  public int familyOf() {
    return Address.familyOf(this.address);
  }

}
