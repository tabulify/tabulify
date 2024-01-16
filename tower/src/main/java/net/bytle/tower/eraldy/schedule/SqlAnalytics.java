package net.bytle.tower.eraldy.schedule;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.healthchecks.Status;
import io.vertx.pgclient.PgPool;
import net.bytle.exception.InternalException;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.vertx.DateTimeUtil;
import net.bytle.vertx.JacksonMapperManager;
import net.bytle.vertx.Server;

import java.io.InputStream;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Scanner;


public class SqlAnalytics implements Handler<Long> {

  private static final String SQL_RESOURCES_PATH = "/analytics/RealmAnalytics.sql";
  private final EraldyApiApp apiApp;
  private LocalDateTime executionLastTime = LocalDateTime.now();

  /**
   * Run the analytics regularly
   */
  public SqlAnalytics(EraldyApiApp apiApp) {
    this.apiApp = apiApp;
    long delayMsEveryHour = 1000 * 60 * 60;
    Server server = apiApp.getApexDomain().getHttpServer().getServer();
    server.getVertx().setPeriodic(delayMsEveryHour, this);
    server.getServerHealthCheck()
      .register(SqlAnalytics.class, promise -> {

        /**
         * Test
         */
        Duration agoLastExecution = Duration.between(this.executionLastTime, LocalDateTime.now());
        boolean executionTest = agoLastExecution.toMillis() <= delayMsEveryHour + 1000;

        /**
         * Data
         * Bug? even if we have added the LocalDateTime Jackson Time module, we get an error
         * {@link JacksonMapperManager}
         * We do it manually then
         */
        String executionsLastTimeString = DateTimeUtil.LocalDateTimetoString(this.executionLastTime);
        JsonObject data = new JsonObject();
        data.put("execution-last-time", executionsLastTimeString);
        data.put("execution-last-ago-sec", agoLastExecution.toSeconds());

        /**
         * Checks and Status Report
         */
        // ok
        if (executionTest) {
          promise.complete(Status.OK(data));
          return;
        }
        // not ok
        Status status = Status.KO();
        data.put("message", "The last time execution date is too old.");
        status.setData(data);
        promise.complete(status);
      });

  }


  @Override
  public void handle(Long event) {
    this.executionLastTime = LocalDateTime.now();
    PgPool jdbcPool = this.apiApp.getApexDomain().getHttpServer().getServer().getJdbcPool();
    InputStream inputStream = SqlAnalytics.class.getResourceAsStream(SQL_RESOURCES_PATH);
    if (inputStream == null) {
      throw new InternalException("The Realm Analytics file was not found");
    }
    Scanner s = new Scanner(inputStream).useDelimiter("\\A");
    String sql = s.hasNext() ? s.next() : "";
    jdbcPool.preparedQuery(sql)
      .execute()
      .onFailure(e -> {
        throw new InternalException("Error on analytics update. Sql: " + sql, e);
      });
  }


}
