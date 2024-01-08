package net.bytle.vertx;

import io.vertx.core.Future;
import io.vertx.core.dns.DnsClientOptions;
import net.bytle.dns.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class TowerDnsClient {

  private static final String DNS_RESOLVER_HOST = "dns.resolver.host";
  static Logger LOGGER = LogManager.getLogger(TowerDnsClient.class);

  private final io.vertx.core.dns.DnsClient client;

  public TowerDnsClient(Server server) {
    DnsClientOptions dnsClientOptions = new DnsClientOptions();
    dnsClientOptions.setQueryTimeout(30000);
    String dnsResolver = server.getConfigAccessor().getString(DNS_RESOLVER_HOST);
    if (dnsResolver != null) {
      LOGGER.info("Dns resolver host set with the value (" + dnsResolver + ") of the configuration (" + DNS_RESOLVER_HOST + ")");
      dnsClientOptions.setHost(dnsResolver);
    } else {
      LOGGER.info("Dns resolver host set to the OS value because the configuration (" + DNS_RESOLVER_HOST + ") was not found");
    }
    this.client = server.getVertx().createDnsClient(dnsClientOptions);
  }

  public Future<Set<DnsMxRecord>> resolveMx(DnsName dnsName) {


    return this.client.resolveMX(dnsName.toStringWithoutRoot())
      .compose(mxRecords -> {
          Set<DnsMxRecord> dnsMxRecords = mxRecords.stream()
            .map(mx -> new DnsMxRecord() {
              @Override
              public int getPriority() {
                return mx.priority();
              }

              @Override
              public DnsName getTarget() {
                try {
                  return DnsName.create(mx.name());
                } catch (DnsIllegalArgumentException e) {
                  throw new RuntimeException(e);
                }
              }
            })
            .collect(Collectors.toSet());
          return Future.succeededFuture(dnsMxRecords);
        },
        err -> {
          /**
           * NXDomain (ie not found)
           */
          if(
            err instanceof io.vertx.core.dns.DnsException &&
              ((io.vertx.core.dns.DnsException) err).code().name().equals("NXDOMAIN")
          ){
            return Future.succeededFuture(new HashSet<>());
          }
          return Future.failedFuture(new DnsException("Error while resolving the Mx records for the domain (" + dnsName + "). Message:" + err.getMessage(), err));
        }
      );
  }


  public Future<Set<DnsIp>> resolveA(DnsName dnsName) {
    return this.client
      .resolveA(dnsName.toStringWithoutRoot())
      .compose(
        ipStrings -> {
          Set<DnsIp> dnsIp = new HashSet<>();
          for (String ipString : ipStrings) {
            try {
              dnsIp.add(DnsIp.createFromString(ipString));
            } catch (DnsException e) {
              return Future.failedFuture(new DnsException("The A record (" + ipString + ") of (" + dnsName + ") is not a valid ip.", e));
            }
          }
          return Future.succeededFuture(dnsIp);
        },
        err -> {
          /**
           * NXDomain (ie not found)
           */
          if(
            err instanceof io.vertx.core.dns.DnsException &&
            ((io.vertx.core.dns.DnsException) err).code().name().equals("NXDOMAIN")
          ){
            return Future.succeededFuture(new HashSet<>());
          }
          return Future.failedFuture(new DnsException("Error while resolving the A records for the domain (" + dnsName + "). Message:" + err.getMessage(), err));
        }
      );
  }

}
