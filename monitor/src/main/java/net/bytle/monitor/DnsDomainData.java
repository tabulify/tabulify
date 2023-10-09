package net.bytle.monitor;

import org.xbill.DNS.Name;
import org.xbill.DNS.TextParseException;

public class DnsDomainData {


  private final String name;
  private final Name dnsName;

  public DnsDomainData(String name) throws TextParseException {

    this.name = name;
    this.dnsName = Name.fromString(this.name + ".");
  }

  public static DnsDomainData create(String name) throws TextParseException {
    return new DnsDomainData(name);
  }

  public Name getName() {
    return this.dnsName;
  }

}
