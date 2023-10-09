package net.bytle.dns;

import org.xbill.DNS.*;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutionException;

public class DnsName {


  private final String name;
  private Resolver resolver;

  public DnsName(String name) {
    this.name = name;

  }

  public static DnsName createFrom(String name) {
    return new DnsName(name);
  }

  public InetAddress getIpAddress() throws UnknownHostException {
    return Address.getByName(name);
  }

  public Name toAbsolute() {
    try {
      return Name.fromString(this.name + DnsUtil.ABSOLUTE_TRAILING_DOT);
    } catch (TextParseException e) {
      throw new RuntimeException(e);
    }
  }

  public ARecord getARecord() throws ExecutionException, InterruptedException {

    Record queryRecord = Record.newRecord(toAbsolute(), Type.A, DClass.IN);
    Message queryMessage = Message.newQuery(queryRecord);


    return getDnsResolver().sendAsync(queryMessage)
      .whenComplete(
        (answer, ex) -> {
          if (ex != null) {
            ex.printStackTrace();
          }
        })
      .toCompletableFuture()
      .get()
      .getSection(1)
      .stream().map(ARecord.class::cast)
      .findFirst()
      .orElseThrow();

  }

  private Resolver getDnsResolver() {
    if (this.resolver == null) {
      return DnsResolver.getLocal();
    }
    return this.resolver;
  }

  public ARecord forwardLookup() throws ExecutionException, InterruptedException {

    return getARecord();

  }

  @SuppressWarnings("unused")
  public void setResolver(Resolver resolver) {
    this.resolver = resolver;
  }

}
