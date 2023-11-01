package net.bytle.ip.api;

import io.vertx.core.Future;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.Tuple;
import net.bytle.exception.NotFoundException;
import net.bytle.ip.IpApp;
import net.bytle.ip.IpVerticle;
import net.bytle.ip.model.IpInfo;
import net.bytle.type.Ip;
import net.bytle.vertx.HttpHeaders;
import net.bytle.vertx.RoutingContextWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * See also
 * <a href="https://ipapi.co/json">...</a>
 */
public class IpApiImpl implements IpApi {


  private static final Logger LOGGER = LoggerFactory.getLogger(IpApiImpl.class);



  //@Override
  public Future<IpInfo> ipGet(RoutingContextWrapper routingContext) {
    String ip;
    try {
      ip = routingContext.getRealRemoteClientIp();
    } catch (NotFoundException e) {
      return Future.failedFuture(e);
    }
    return this.ipIpGet(routingContext, ip);
  }

  //@Override
  public Future<IpInfo> ipIpGet(RoutingContextWrapper routingContext, String ip) {

    routingContext.response().putHeader("Content-Type", "application/json");
    routingContext.response().putHeader(HttpHeaders.CACHE_CONTROL, HttpHeaders.CACHE_CONTROL_NO_STORE);

    final String ipv4;
    if (ip.equals("0:0:0:0:0:0:0:1")) {
      ipv4 = ip = "127.0.0.1";
    } else {
      ipv4 = ip;
    }
    Long numericIp = Ip.ipv4ToLong(ip);
    LOGGER.info("numericIp is {}", numericIp);
    PgPool jdbcPool = IpVerticle.server.getJdbcPool();
    // One shot, no need to close anything and return only one row
    // https://vertx.io/docs/apidocs/io/vertx/ext/sql/SQLOperations.html#querySingleWithParams-java.lang.String-io.vertx.core.json.JsonArray-io.vertx.core.Handler-
    String sql = "SELECT * FROM " + IpApp.CS_IP_SCHEMA + ".ip " +
      "WHERE " +
      "ip_from <= $1 " +
      "and ip_to >= $2";
    return jdbcPool.preparedQuery(sql)
      .execute(Tuple.of(numericIp, numericIp))
      .onFailure(e -> LOGGER.error("Database query error with Sql:\n" + sql, e))
      .compose(rows -> {
        LOGGER.info("Fetch succeeded for IP {}", numericIp);
        IpInfo ipResponse = new IpInfo();
        if (rows.size() != 0) {

          Row row = rows.iterator().next();
          LOGGER.info("Query fetched {}", row);
          ipResponse.setCountry2(row.getString(4));
          ipResponse.setCountry3(row.getString(5));
          ipResponse.setCountry(row.getString(6));
          ipResponse.setIp(ipv4);

        }

        return Future.succeededFuture(ipResponse);
      });


  }

}
