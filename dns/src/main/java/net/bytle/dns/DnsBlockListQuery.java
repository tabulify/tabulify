package net.bytle.dns;

import net.bytle.exception.IllegalStructure;
import net.bytle.exception.InternalException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;

/**
 * Perform IP and Domain query against block list DSN zone
 */
public class DnsBlockListQuery {

  static Logger LOGGER = LogManager.getLogger(DnsBlockListQuery.class);
  private final BuilderConf conf;

  private final HashMap<DnsBlockList, DnsBlockListResponseCode> responses;


  public DnsBlockListQuery(BuilderConf conf) {
    this.conf = conf;
    this.responses = new HashMap<>();
    for (DnsBlockList dnsBlockList : this.conf.blockLists) {
      if (dnsBlockList.getType() != this.conf.dnsBlockListType) {
        continue;
      }
      this.responses.put(dnsBlockList, query(dnsBlockList));
    }
  }

  public static BuilderConf forDomain(String domain) {
    return new BuilderConf(DnsBlockListType.DOMAIN, domain);
  }


  /**
   * ***************************************************************************
   * IP Query:
   * as explained here:
   * <a href="https://en.wikipedia.org/wiki/Domain_Name_System_blocklist#DNSBL_queries">...</a>
   * and here:
   * <a href="https://www.spamhaus.org/faq/section/DNSBL%20Usage#366">How do I check my DNS server results?</a>
   * For the reason:
   * You can look up the name as a text record ("TXT" record).
   * Most DNSBLs publish information about why a client is listed as TXT records.
   * Doc: https://www.spamhaus.org/faq/section/DNSBL%20Usage#200
   * ***************************************************************************
   * Domain Query:
   * As explained here:
   * <a href="https://en.wikipedia.org/wiki/Domain_Name_System_blocklist#URI_DNSBL">...</a>
   * <p>
   * Can also be used for web app:
   * <a href="  * https://www.spamhaus.org/faq/section/DNSBL%20Usage#252">Can a Spamhaus DNSBL
   * be used on a web server or other applications?</a>
   */
  private DnsBlockListResponseCode query(DnsBlockList dnsBlockList) {

    try {

      String endPartBlockZone = "." + dnsBlockList.getZone();
      String host = this.conf.queryTerm + endPartBlockZone;
      /**
       * InetAddress uses the local DNS of the machine
       * Unfortunately, it does not work for public DNS (ie VPS in the cloud)
       * A DNS server (such as Bind) should be installed
       * Bind (Berkeley Internet Name Domain)
       */
      InetAddress responseAddress = InetAddress.getByName(host);

      Set<DnsBlockListResponseCode> blockingKnownResponses = dnsBlockList.getBlockingKnownResponses();
      if (blockingKnownResponses.size() == 0) {
        /**
         * Response may be error response, we don't know, normally
         * you are blocked if we get a response
         */
        LOGGER.info("No known responses for the blocking list. We block because we got the DNS response (" + responseAddress + ") for the host (" + host + ")");
        return DnsBlockListResponseCode.R_INTERNAL_HOST_BLOCKED;
      }
      /**
       * Not all responses are blocking response
       * If we know the blocking responses, we check
       */
      String responseCode = responseAddress.getHostAddress().replace(endPartBlockZone, "");
      DnsBlockListResponseCode response = blockingKnownResponses.stream()
        .filter(r -> r.getResponseCode().equals(responseCode))
        .findFirst()
        .orElse(null);
      if (response == null) {
        // Error on our end
        LOGGER.warn("Internal Error: No response code for the response (" + responseCode + ")");
        return DnsBlockListResponseCode.R_INTERNAL_RESPONSE_NOT_IN_LIST;
      }
      if (!response.getBlocked()) {
        LOGGER.warn("Dns Blocking Query Error: Response code: " + response.getResponseCode() + ", description: " + response.getDescription());
      }
      return response;
    } catch (UnknownHostException e) {
      return DnsBlockListResponseCode.R_INTERNAL_HOST_NOT_BLOCKED;
    }

  }

  public static BuilderConf forIp(String ipv4Address) throws IllegalStructure {
    if (ipv4Address == null) {
      throw new IllegalStructure("Ip Remote Address should not be null");
    }
    List<String> ipOctets = Arrays.asList(ipv4Address.split("\\."));
    if (ipOctets.size() != 4) {
      throw new IllegalStructure("This is not an IPv4 address");
    }
    Collections.reverse(ipOctets);
    String ipToCheckReversed = String.join(".", ipOctets);
    return new BuilderConf(DnsBlockListType.IP, ipToCheckReversed);
  }


  public DnsBlockListResponseCode getFirst() {
    return this.responses
      .values()
      .stream()
      .findFirst()
      .orElseThrow();
  }

  public Map.Entry<DnsBlockList, DnsBlockListResponseCode> getFirstRecord() {
    return this.responses.entrySet()
      .stream()
      .findFirst()
      .orElseThrow();
  }

  public Map<DnsBlockList, DnsBlockListResponseCode> getResponses() {
    return this.responses;
  }

  public static class BuilderConf {
    private final DnsBlockListType dnsBlockListType;
    private final String queryTerm;
    private final List<DnsBlockList> blockLists = new ArrayList<>();

    public BuilderConf(DnsBlockListType dnsBlockListType, String queryTerm) {
      this.dnsBlockListType = dnsBlockListType;
      this.queryTerm = queryTerm;
    }

    @SuppressWarnings("unused")
    public DnsBlockListQuery.BuilderConf addAllBlockLists(List<DnsBlockList> blockLists) {
      for (DnsBlockList blockList : blockLists) {
        this.blockLists.addAll(blockLists);
      }
      return this;
    }

    @SuppressWarnings("unused")
    public DnsBlockListQuery.BuilderConf addBlockList(DnsBlockList blockList) {
      addAllBlockLists(Collections.singletonList(blockList));
      return this;
    }

    public DnsBlockListQuery query() {
      if (this.blockLists.size() == 0) {
        switch (this.dnsBlockListType) {
          case IP:
            this.addBlockList(DnsBlockList.ZEN_SPAMHAUS_ORG);
            break;
          case DOMAIN:
            this.addBlockList(DnsBlockList.DBL_SPAMHAUS_ORG);
            break;
          default:
            throw new InternalException("The query type is unknown");
        }
      }
      return new DnsBlockListQuery(this);
    }
  }
}
