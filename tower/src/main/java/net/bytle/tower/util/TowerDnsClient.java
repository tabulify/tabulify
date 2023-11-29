package net.bytle.tower.util;

import io.vertx.core.Future;
import io.vertx.core.dns.DnsClientOptions;
import net.bytle.dns.DnsException;
import net.bytle.dns.DnsMxRecord;
import net.bytle.dns.DnsName;
import net.bytle.dns.DnsNotFoundException;
import net.bytle.vertx.Server;
import net.bytle.vertx.TowerApp;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

public class TowerDnsClient {

  private static final String DNS_RESOLVER_HOST = "dns.resolver.host";
  static Logger LOGGER = LogManager.getLogger(TowerDnsClient.class);

  private final io.vertx.core.dns.DnsClient client;

  public TowerDnsClient(TowerApp towerApp) {
    Server server = towerApp.getApexDomain().getHttpServer().getServer();
    DnsClientOptions dnsClientOptions =new DnsClientOptions();
    String dnsResolver = server.getConfigAccessor().getString(DNS_RESOLVER_HOST);
    if(dnsResolver!=null) {
      LOGGER.info("Dns resolver host set with the value ("+dnsResolver+") of the configuration ("+DNS_RESOLVER_HOST+")");
      dnsClientOptions.setHost(dnsResolver);
    } else {
      LOGGER.info("Dns resolver host set to the OS value because the configuration ("+DNS_RESOLVER_HOST+") was not found");
    }
    this.client = server.getVertx().createDnsClient(dnsClientOptions);
  }

  public Future<List<String>> resolveTxt(DnsName dnsName) throws DnsException, DnsNotFoundException {
    return null;
  }

  public List<DnsMxRecord> resolveMx(DnsName dnsName) throws DnsNotFoundException, DnsException {
    return null;
  }


}
