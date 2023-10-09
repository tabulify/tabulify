package net.bytle.tower.eraldy.app.combopublicapi.implementer;

import io.vertx.core.Future;
import io.vertx.ext.web.RoutingContext;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.Tuple;
import net.bytle.exception.NotFoundException;
import net.bytle.network.Ip;
import net.bytle.tower.eraldy.app.combopublicapi.openapi.interfaces.IpPublicapi;
import net.bytle.tower.eraldy.app.combopublicapi.openapi.invoker.ApiResponse;
import net.bytle.tower.eraldy.model.openapi.IpInfo;
import net.bytle.tower.util.HttpHeaders;
import net.bytle.tower.util.HttpRequestUtil;
import net.bytle.tower.util.JdbcPoolCs;
import net.bytle.tower.util.JdbcSchemaManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * See also
 * <a href="https://ipapi.co/json">...</a>
 */
public class IpPublicapiImpl implements IpPublicapi {


  public static final String CS_IP_SCHEMA = JdbcSchemaManager.SCHEMA_PREFIX + "ip";
  private static final Logger LOGGER = LoggerFactory.getLogger(IpPublicapiImpl.class);

  //@Override
  public Future<ApiResponse<IpInfo>> ipGet(RoutingContext routingContext) {
    String ip;
    try {
      ip = HttpRequestUtil.getRealRemoteClientIp(routingContext.request());
    } catch (NotFoundException e) {
      ApiResponse<IpInfo> apiResponse = new ApiResponse<>(500);
      return Future.succeededFuture(apiResponse);
    }
    return this.ipIpGet(routingContext, ip);
  }

  //@Override
  public Future<ApiResponse<IpInfo>> ipIpGet(RoutingContext routingContext, String ip) {

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
    PgPool jdbcPool = JdbcPoolCs.getJdbcPool(routingContext.vertx());
    // One shot, no need to close anything and return only one row
    // https://vertx.io/docs/apidocs/io/vertx/ext/sql/SQLOperations.html#querySingleWithParams-java.lang.String-io.vertx.core.json.JsonArray-io.vertx.core.Handler-
    String sql = "SELECT * FROM " + CS_IP_SCHEMA + ".ip " +
      "WHERE " +
      "ip_from <= $1 " +
      "and ip_to >= $2";
    return jdbcPool.preparedQuery(sql)
      .execute(Tuple.of(numericIp, numericIp))
      .onFailure(e -> {
        // handle the failure
        LOGGER.error("Database query error with Sql:\n" + sql, e);
        routingContext.fail(e);
      })
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
        ApiResponse<IpInfo> ipInfoApiResponse = new ApiResponse<>(200, ipResponse);
        return Future.succeededFuture(ipInfoApiResponse);
      });


  }
}
