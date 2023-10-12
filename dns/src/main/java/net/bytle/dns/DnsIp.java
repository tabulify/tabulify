package net.bytle.dns;

import net.bytle.exception.NotFoundException;
import org.xbill.DNS.*;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutionException;

public class DnsIp {


  private final String ip;
  private final InetAddress address;
  private final DnsSession dnsSession;

  public DnsIp(DnsSession session, String ipv) throws UnknownHostException {
    this.ip = ipv;
    this.dnsSession = session;

    this.address = Address.getByAddress(ipv);

  }


  public InetAddress getInetAddress() {
    return this.address;
  }

  @Override
  public String toString() {
    return ip;
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
