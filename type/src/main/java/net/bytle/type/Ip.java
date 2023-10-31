package net.bytle.type;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

public class Ip {

  /**
   * Same as Math.pow(256,i)
   */
  public static final Long[] FACTOR_BY_POSITION = new Long[]{256 * 256 * 256L, 256 * 256L, 256L, 1L};

  /**
   * 1.2.3.4 = 4 + (3 * 256) + (2 * 256 * 256) + (1 * 256 * 256 * 256)
   * is 4 + 768 + 13,1072 + 16,777,216 = 16,909,060
   * <p>
   * Integer in java are stored on 32 bits, the same as an IP.
   *
   * @param ip a ipv4 string
   * @return the numeric representation
   */
  public static long ipv4ToLong(String ip) {
    String[] ipParts = ip.split("\\.");
    return IntStream.range(0, ipParts.length)
      .mapToLong(i -> Integer.parseInt(ipParts[i]) * FACTOR_BY_POSITION[i])
      .sum();
  }

  /**
   * Adapted
   * from <a href="https://dba.stackexchange.com/questions/134643/how-to-convert-decimal-number-to-ip-address">...</a>
   * <p>
   * four octets written in decimal numbers, ranging from 0 to 255
   *
   * @param ip in long format
   * @return an IP in
   */
  static protected String LongToIpv4(Long ip) {
    List<String> stringIp = new ArrayList<>();
    Long ipProcessing = ip;
    for (Long factor:FACTOR_BY_POSITION) {
      Long octet = ipProcessing / factor;
      ipProcessing = ipProcessing - octet * factor;
      stringIp.add(String.valueOf(octet));
    }
    return String.join(".",stringIp);
  }

}
