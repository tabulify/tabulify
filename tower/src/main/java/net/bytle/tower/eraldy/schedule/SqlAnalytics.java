package net.bytle.tower.eraldy.schedule;

import io.vertx.core.Handler;
import io.vertx.pgclient.PgPool;
import net.bytle.exception.InternalException;
import net.bytle.tower.eraldy.EraldyDomain;
import net.bytle.vertx.JdbcPostgresPool;

import java.io.InputStream;
import java.util.Scanner;

public class SqlAnalytics implements Handler<Long> {

  private static final String SQL_RESOURCES_PATH = "/analytics/RealmAnalytics.sql";

  public SqlAnalytics(EraldyDomain eraldyDomain) {
    long delayMsEveryHour = 1000 * 60 * 60;
    eraldyDomain.getVertx().setPeriodic(delayMsEveryHour, this);
  }

  public static SqlAnalytics create(EraldyDomain eraldyDomain) {
    return new SqlAnalytics(eraldyDomain);
  }

  @Override
  public void handle(Long event) {
    PgPool jdbcPool = JdbcPostgresPool.getJdbcPool();
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
