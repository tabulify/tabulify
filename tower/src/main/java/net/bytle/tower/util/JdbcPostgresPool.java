package net.bytle.tower.util;

import io.vertx.core.Vertx;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.PoolOptions;
import net.bytle.exception.InternalException;
import org.postgresql.ds.PGSimpleDataSource;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * A wrapper around {@link PgPool} that handle the
 * creation (and the {@link JdbcSchemaManager}
 * <p>
 * <p>
 * A JdbcPool is not dependent of any {@link io.vertx.core.Verticle} as they
 * can be shutdown. It depends on {@link Vertx}
 */
public class JdbcPostgresPool {


  private static PgPool jdbcPool = null;
  private final PGSimpleDataSource dataSource;

  /**
   * @return the JDBCPool
   * @throws InternalException - a runtime internal error if not found
   *                           we don't throw an Exception at compile time because it should not happen
   */
  public static PgPool getJdbcPool() {
    if (jdbcPool == null) {
      throw new InternalException("No Jdbc Pool found for this vertx");
    }
    return jdbcPool;
  }



  public JdbcPostgresPool(Vertx vertx, JdbcConnectionInfo jdbcConnectionInfo) {

    dataSource = new PGSimpleDataSource();
    dataSource.setURL(jdbcConnectionInfo.getUrl());
    String user = jdbcConnectionInfo.getUser();
    dataSource.setUser(user);
    String password = jdbcConnectionInfo.getPassword();
    dataSource.setPassword(password);

    // Set the working schema
    // https://vertx.io/docs/vertx-pg-client/java/#_data_object
    // https://www.postgresql.org/docs/current/runtime-config-client.html
    Map<String, String> connectionProps = new HashMap<>();
    connectionProps.put("search_path", jdbcConnectionInfo.getSchemaPath());


    String postgresUri = jdbcConnectionInfo.getPostgresUri();
    PgConnectOptions pgConnectOptions = PgConnectOptions.fromUri(postgresUri)
      .setUser(user)
      .setPassword(jdbcConnectionInfo.getPassword())
      .setProperties(connectionProps);

    PoolOptions poolOptions = new PoolOptions().setMaxSize(jdbcConnectionInfo.getMaxPoolSize());

    /**
     * Then create the pool and return
     */
    jdbcPool = PgPool.pool(
      vertx,
      pgConnectOptions,
      poolOptions
    );


  }

  public static JdbcPostgresPool createFromJdbcConnectionInfo(Vertx vertx, JdbcConnectionInfo jdbcConnectionInfo) {
    return new JdbcPostgresPool(vertx, jdbcConnectionInfo);
  }


  public DataSource getDataSource() {
    return this.dataSource;
  }

}
