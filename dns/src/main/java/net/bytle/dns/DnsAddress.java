package net.bytle.dns;

import org.xbill.DNS.*;
import org.xbill.DNS.lookup.LookupSession;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutionException;

public class DnsAddress {
  private final InetAddress address;
  private final Resolver resolver;

  public DnsAddress(InetAddress byAddress) {
    this.address = byAddress;
    this.resolver = DnsResolver.getLocal();
  }

  public static DnsAddress createFromIpv4String(String number) throws UnknownHostException {
    return new DnsAddress(Address.getByAddress(number, Address.IPv4));
  }

  public static DnsAddress createFromIpv6String(String number) throws UnknownHostException {
    return new DnsAddress(Address.getByAddress(number, Address.IPv6));
  }

  public InetAddress getAddress() {
    return this.address;
  }

  public PTRRecord getPtrRecordAsync() throws ExecutionException, InterruptedException {

    Name name = ReverseMap.fromAddress(this.address);
    Record queryRecord = Record.newRecord(name, Type.PTR, DClass.IN);
    Message queryMessage = Message.newQuery(queryRecord);

    return (PTRRecord) resolver.sendAsync(queryMessage)
      .whenComplete(
        (answer, ex) -> {
          if (ex != null) {
            ex.printStackTrace();
          }
        })
      .toCompletableFuture()
      .get()
      .getSection(1)
      .get(0);

  }

  @SuppressWarnings("unused")
  public PTRRecord reversePTR() throws ExecutionException, InterruptedException {
    return getPtrRecordAsync();
  }

  @SuppressWarnings("unused")
  public PTRRecord getPtrRecordAsyncWithSession() throws ExecutionException, InterruptedException {

    LookupSession lookupSession = LookupSession
      .builder()
      .resolver(DnsResolver.getLocal())
      .build();

    Name name = ReverseMap.fromAddress(this.address);

    return lookupSession.lookupAsync(name, Type.PTR)
      .whenComplete(
        (answer, ex) -> {
          if (ex != null) {
            ex.printStackTrace();
          }
        })
      .toCompletableFuture()
      .get()
      .getRecords()
      .stream()
      .map(PTRRecord.class::cast)
      .findFirst()
      .orElseThrow();
  }
}
