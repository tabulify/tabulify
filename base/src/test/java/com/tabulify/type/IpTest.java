package com.tabulify.type;

import org.junit.Assert;
import org.junit.Test;

public class IpTest {

  @Test
  public void translation() {
    long decimalIp = 37228544L;
    String ipv4Processed = Ip.LongToIpv4(decimalIp);
    String ipv4Original = "2.56.16.0";
    Assert.assertEquals(ipv4Original,ipv4Processed);

    long ipv4 = Ip.ipv4ToLong(ipv4Original);
    Assert.assertEquals(decimalIp, ipv4);
  }

  @Test
  public void ipToMinNumeric() {
    String ip = "1.2.3.4";
    long expected = 16909060L;
    Assert.assertEquals(expected, Ip.ipv4ToLong(ip));
  }

  @Test
  public void ipToMaxNumeric() {
    String ip = "256.256.256.256";
    long expected = 256L + (256 * 256L) + (256 * 256 * 256L) + (256 * 256 * 256 * 256L);
    Assert.assertEquals(expected, Ip.ipv4ToLong(ip));
  }

}
