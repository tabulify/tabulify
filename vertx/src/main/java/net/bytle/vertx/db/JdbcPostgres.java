package net.bytle.vertx.db;

import io.vertx.core.Vertx;
import io.vertx.pgclient.PgBuilder;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.PoolOptions;
import net.bytle.exception.NullValueException;
import net.bytle.vertx.Server;
import org.postgresql.ds.PGSimpleDataSource;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * A wrapper around Vertx JDBC Postgres that handle the
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

  public JdbcPostgres(Server server, String confPrefix, String appName) {

    super(server);

    jdbcConnectionInfo = JdbcConnectionInfo.createFromJson(confPrefix, server.getConfigAccessor());
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
    // application_name and not ApplicationName
    // as seen in PgConnectOptions default properties
    connectionProps.put("application_name", appName);


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

  public static JdbcClient create(Server vertx, String confPrefix, String appName) {

    return new JdbcPostgres(vertx, confPrefix, appName);

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
