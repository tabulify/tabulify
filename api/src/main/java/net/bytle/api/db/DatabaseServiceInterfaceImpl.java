package net.bytle.api.db;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.stream.IntStream;

public class DatabaseServiceInterfaceImpl implements DatabaseServiceInterface {

  private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseServiceInterfaceImpl.class);

  private final JDBCClient dbClient;

  public DatabaseServiceInterfaceImpl(JDBCClient dbClient, JsonObject jsonObject, Handler<AsyncResult<DatabaseServiceInterface>> readyHandler) {
    this.dbClient = dbClient;

    // Migrate if not done
    // https://flywaydb.org/documentation/api/
    try {
      String url = jsonObject.getString("url");
      Flyway flyway = Flyway.configure().dataSource(url,null,null).load();
      flyway.migrate();
    } catch (FlywayException e) {
      LOGGER.error("Database preparation error", e.getCause());
      readyHandler.handle(Future.failedFuture(e.getCause()));
    }
    readyHandler.handle(Future.succeededFuture(this));

  }

  @Override
  public DatabaseServiceInterface getIp(String ip, Handler<AsyncResult<JsonObject>> resultHandler) {
    Integer numericIp = getNumericIp(ip);
    JsonArray params = new JsonArray()
      .add(numericIp)
      .add(numericIp);
    // One shot, no need to close anything and return only one row
    // https://vertx.io/docs/apidocs/io/vertx/ext/sql/SQLOperations.html#querySingleWithParams-java.lang.String-io.vertx.core.json.JsonArray-io.vertx.core.Handler-
    dbClient.querySingleWithParams("SELECT * FROM IP WHERE ip_from <= ? and ip_to >= ?", params, fetch -> {
      if (fetch.succeeded()) {
        JsonArray row = fetch.result();
        JsonObject response = new JsonObject();
        if (row == null) {
          response.put("found", false);
        } else {
          response.put("found", true);
          response.put("country2", row.getString(4));
          response.put("country3", row.getString(5));
          response.put("country", row.getString(6));
        }
        resultHandler.handle(Future.succeededFuture(response));
      } else {
        LOGGER.error("Database query error", fetch.cause());
        resultHandler.handle(Future.failedFuture(fetch.cause()));
      }
    });
    // Fluent
    return this;
  }

  private void reportQueryError(Message<JsonObject> message, Throwable cause) {
    LOGGER.error("Database query error", cause);
    message.fail(ErrorCodes.DB_ERROR.ordinal(), cause.getMessage());
  }

  /**
   * 1.2.3.4 = 4 + (3 * 256) + (2 * 256 * 256) + (1 * 256 * 256 * 256)
   * is 4 + 768 + 13,1072 + 16,777,216 = 16,909,060
   *
   * @param ip
   * @return the numeric representation
   */
  static protected Integer getNumericIp(String ip) {
    Integer[] factorByPosition = {256 * 256 * 256, 256 * 256, 256, 1};
    String[] ipParts = ip.split("\\.");
    return IntStream.range(0, ipParts.length)
      .map(i -> Integer.parseInt(ipParts[i]) * factorByPosition[i])
      .sum();
  }

  public enum ErrorCodes {
    NO_ACTION_SPECIFIED,
    BAD_ACTION,
    DB_ERROR
  }

}
