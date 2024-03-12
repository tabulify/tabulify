package net.bytle.vertx;

import io.vertx.core.Vertx;
import io.vertx.pgclient.PgBuilder;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.PoolOptions;
import net.bytle.exception.NullValueException;
import org.postgresql.ds.PGSimpleDataSource;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * A wrapper around Vertx JDBC Postgress that handle the
 * creation (and the {@link JdbcSchemaManager}
 * <p>
 * <p>
 * A JdbcPool is not dependent of any {@link io.vertx.core.Verticle} as they
 * can be shutdown. It depends on {@link Vertx}
 */
public class JdbcPostgres extends JdbcClient {


  private final JdbcSchemaManager pgSchemaManager;
  private final JdbcConnectionInfo jdbcConnectionInfo;
  private final Pool pool;

  public JdbcPostgres(Server server, String connectionInfoPrefix) {

    super(server);

    jdbcConnectionInfo = JdbcConnectionInfo.createFromJson(connectionInfoPrefix, server.getConfigAccessor());
    pgSchemaManager = JdbcSchemaManager.create(this);

    String user = jdbcConnectionInfo.getUser();

    // Set the working schema
    // https://vertx.io/docs/vertx-pg-client/java/#_data_object
    // https://www.postgresql.org/docs/current/runtime-config-client.html
    Map<String, String> connectionProps = new HashMap<>();
    try {
      String schemaPath = jdbcConnectionInfo.getSchemaPath();
      connectionProps.put("search_path", schemaPath);
    } catch (NullValueException e) {
      // not set, null
    }


    String postgresUri = jdbcConnectionInfo.getPostgresUri();
    PgConnectOptions pgConnectOptions = PgConnectOptions.fromUri(postgresUri)
      .setUser(user)
      .setPassword(jdbcConnectionInfo.getPassword())
      .setConnectTimeout(jdbcConnectionInfo.getConnectTimeout())
      .setProperties(connectionProps);

    PoolOptions poolOptions = new PoolOptions()
      .setMaxSize(jdbcConnectionInfo.getMaxPoolSize())
      .setConnectionTimeout(PoolOptions.DEFAULT_CONNECTION_TIMEOUT)
      .setConnectionTimeoutUnit(PoolOptions.DEFAULT_CONNECTION_TIMEOUT_TIME_UNIT);


    this.pool = PgBuilder
      .pool()
      .with(poolOptions)
      .connectingTo(pgConnectOptions)
      .using(server.getVertx())
      .build();

  }

  public static JdbcClient create(Server vertx, String jdbcConnectionInfo) {

    return new JdbcPostgres(vertx, jdbcConnectionInfo);

  }

  @Override
  public JdbcSchemaManager getSchemaManager() {
    return pgSchemaManager;
  }

  @Override
  public Pool getPool() {
    return pool;
  }

  @Override
  public void close() throws Exception {
    this.pool.close();
  }

  @Override
  public JdbcConnectionInfo getConnectionInfo() {
    return this.jdbcConnectionInfo;
  }

  @Override
  public DataSource getDataSource() {
    PGSimpleDataSource dataSource = new PGSimpleDataSource();
    dataSource.setConnectTimeout(this.jdbcConnectionInfo.getConnectTimeout());
    dataSource.setSocketTimeout(this.jdbcConnectionInfo.getConnectTimeout());
    dataSource.setLoginTimeout(this.jdbcConnectionInfo.getConnectTimeout());
    dataSource.setURL(this.jdbcConnectionInfo.getUrl());
    dataSource.setUser(this.jdbcConnectionInfo.getUser());
    dataSource.setPassword(this.jdbcConnectionInfo.getPassword());
    return dataSource;
  }

}
