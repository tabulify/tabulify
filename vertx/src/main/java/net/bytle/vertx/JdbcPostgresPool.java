package net.bytle.vertx;

import io.vertx.core.Vertx;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.PoolOptions;
import net.bytle.exception.NullValueException;

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



  public static PgPool create(Vertx vertx, JdbcConnectionInfo jdbcConnectionInfo) {
    String user = jdbcConnectionInfo.getUser();

    // Set the working schema
    // https://vertx.io/docs/vertx-pg-client/java/#_data_object
    // https://www.postgresql.org/docs/current/runtime-config-client.html
    Map<String, String> connectionProps = new HashMap<>();
    try {
      String schemaPath  = jdbcConnectionInfo.getSchemaPath();
      connectionProps.put("search_path", schemaPath);
    } catch (NullValueException e) {
      // not set, null
    }


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
    return jdbcPool;

  }
}
