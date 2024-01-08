package net.bytle.vertx;

import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.dns.DnsClientOptions;
import net.bytle.dns.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.xbill.DNS.*;
import org.xbill.DNS.lookup.LookupResult;
import org.xbill.DNS.lookup.LookupSession;
import org.xbill.DNS.lookup.NoSuchDomainException;

import java.net.UnknownHostException;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static net.bytle.dns.XBillDnsClient.getXbillName;

public class TowerDnsClient {

  private static final String DNS_ASYNC_RESOLVER_HOST = "dns.async.resolver.host";
  /**
   * xbill or vertx
   */
  private static final String DNS_ASYNC_CLIENT = "dns.async.client";
  private static final String DNS_ASYNC_QUERY_TIMEOUT = "dns.async.query.timeout";
  static Logger LOGGER = LogManager.getLogger(TowerDnsClient.class);
  private final Context vertxContext;

  private io.vertx.core.dns.DnsClient vertxClient;
  private final LookupSession xBillClient;

  public TowerDnsClient(Server server) throws ConfigIllegalException {
    ConfigAccessor configAccessor = server.getConfigAccessor();
    String clientType = configAccessor.getString(DNS_ASYNC_CLIENT, "xbill");
    LOGGER.info("Dns client type is: " + clientType);
    String dnsResolver = configAccessor.getString(DNS_ASYNC_RESOLVER_HOST);
    long timeout = configAccessor.getLong(DNS_ASYNC_QUERY_TIMEOUT, DnsClientOptions.DEFAULT_QUERY_TIMEOUT);
    if (dnsResolver != null) {
      LOGGER.info("Dns resolver host set with the value (" + dnsResolver + ") of the configuration (" + DNS_ASYNC_RESOLVER_HOST + ")");
    } else {
      LOGGER.info("Dns resolver host set to the OS value because the configuration (" + DNS_ASYNC_RESOLVER_HOST + ") was not found");
    }
    switch (clientType) {
      case "vertx":
        DnsClientOptions dnsClientOptions = new DnsClientOptions();
        dnsClientOptions.setQueryTimeout(timeout);
        if (dnsResolver != null) {
          dnsClientOptions.setHost(dnsResolver);
        }
        this.vertxClient = server.getVertx().createDnsClient(dnsClientOptions);
      default:
        SimpleResolver resolver;
        if (dnsResolver == null) {
          resolver = new SimpleResolver(ResolverConfig.getCurrentConfig().server());
        } else {
          try {
            resolver = new SimpleResolver(dnsResolver);
          } catch (UnknownHostException e) {
            throw new ConfigIllegalException("The resolver host (" + dnsResolver + ") does not exist", e);
          }
        }
        this.vertxContext = server.getVertx().getOrCreateContext();
        resolver.setTimeout(Duration.ofMillis(timeout));
        xBillClient = LookupSession
          .builder()
          .resolver(resolver)
          .build();

    }


  }

  public Future<Set<DnsMxRecord>> resolveMx(DnsName dnsName) {

    if (xBillClient != null) {
      Name xbillName;
      try {
        xbillName = getXbillName(dnsName);
      } catch (DnsException e) {
        return Future.failedFuture(e);
      }
      CompletableFuture<LookupResult> completableFuture = xBillClient
        .lookupAsync(xbillName, Type.MX)
        .toCompletableFuture();
      return Future.fromCompletionStage(completableFuture, vertxContext)
        .compose(
          lookupResult -> {
            Set<DnsMxRecord> mxRecords = lookupResult.getRecords()
              .stream()
              .map(MXRecord.class::cast)
              .map(mx -> new DnsMxRecord() {
                @Override
                public int getPriority() {
                  return mx.getPriority();
                }

                @Override
                public DnsName getTarget() {
                  try {
                    return DnsName.create(mx.getName().toString());
                  } catch (DnsIllegalArgumentException e) {
                    throw new RuntimeException(e);
                  }
                }
              })
              .collect(Collectors.toSet());
            return Future.succeededFuture(mxRecords);
          },
          err->{
            if (err.getCause() instanceof NoSuchDomainException) {
              return Future.succeededFuture(new HashSet<>());
            }
            DnsException dnsException = new DnsException("Error while resolving the Mx records for the domain (" + dnsName + "). Message: " + err.getMessage() + " (" + err.getClass().getName() + ")", err);
            return Future.failedFuture(dnsException);
          });

    }

    return this.vertxClient
      .resolveMX(dnsName.toStringWithoutRoot())
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
          if (
            err instanceof io.vertx.core.dns.DnsException &&
              ((io.vertx.core.dns.DnsException) err).code().name().equals("NXDOMAIN")
          ) {
            return Future.succeededFuture(new HashSet<>());
          }
          String errorMessage = "Error while resolving the Mx records for the domain (" + dnsName + "). Message: " + err.getMessage() + " (" + err.getClass().getName() + ")";
          return Future.failedFuture(new DnsException(errorMessage, err));
        }
      );
  }


  public Future<Set<DnsIp>> resolveA(DnsName dnsName) {
    if (xBillClient != null) {
      Name xbillName;
      try {
        xbillName = getXbillName(dnsName);
      } catch (DnsException e) {
        return Future.failedFuture(e);
      }
      CompletableFuture<LookupResult> completableFuture = xBillClient
        .lookupAsync(xbillName, Type.A)
        .toCompletableFuture();
      return Future.fromCompletionStage(completableFuture, vertxContext)
        .compose(
          lookupResult -> {
            Set<DnsIp> IpRecord = lookupResult.getRecords()
              .stream().map(ARecord.class::cast)
              .map(ARecord::getAddress)
              .map(DnsIp::createFromInetAddress)
              .collect(Collectors.toSet());
            return Future.succeededFuture(IpRecord);
          },
          err->{
            if (err.getCause() instanceof NoSuchDomainException) {
              return Future.succeededFuture(new HashSet<>());
            }
            DnsException dnsException = new DnsException("Error while resolving the A records for the domain (" + dnsName + "). Message: " + err.getMessage() + " (" + err.getClass().getName() + ")", err);
            return Future.failedFuture(dnsException);
          });

    }

    return this.vertxClient
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
          if (
            err instanceof io.vertx.core.dns.DnsException &&
              ((io.vertx.core.dns.DnsException) err).code().name().equals("NXDOMAIN")
          ) {
            return Future.succeededFuture(new HashSet<>());
          }
          return Future.failedFuture(new DnsException("Error while resolving the A records for the domain (" + dnsName + "). Message:" + err.getMessage(), err));
        }
      );
  }

}
