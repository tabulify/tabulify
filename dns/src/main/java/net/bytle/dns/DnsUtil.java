package net.bytle.dns;

import org.xbill.DNS.TXTRecord;

public class DnsUtil {


  public static String getStringFromTxtRecord(TXTRecord txtRecord) {
    return String.join("", txtRecord.getStrings());
  }
}
