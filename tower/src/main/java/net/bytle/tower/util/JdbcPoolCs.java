package net.bytle.tower.util;

import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.PoolOptions;
import net.bytle.exception.DbMigrationException;
import net.bytle.exception.InternalException;
import org.postgresql.ds.PGSimpleDataSource;

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
public class JdbcPoolCs implements Handler<Promise<PgPool>> {


  private final JdbcConnectionInfo jdbcConnectionInfo;
  private final Vertx vertx;

  /**
   * @param vertx the vertx
   * @return the JDBCPool
   * @throws InternalException - a runtime internal error if not found
   *                           we don't throw an Exception at compile time because it should not happen
   */
  public static PgPool getJdbcPool(Vertx vertx) {
    PgPool jdbcPool = jdbcMap.get(vertx);
    if (jdbcPool == null) {
      throw new InternalException("No Jdbc Pool found for this vertx");
    }
    return jdbcPool;
  }

  /**
   * A jdbc pool is vertx dependent in it creation
   */
  static private final Map<Vertx, PgPool> jdbcMap = new HashMap<>();


  public JdbcPoolCs(Vertx vertx, JdbcConnectionInfo jdbcConnectionInfo) {

    this.vertx = vertx;
    this.jdbcConnectionInfo = jdbcConnectionInfo;


  }

  public static JdbcPoolCs createFromJdbcConnectionInfo(Vertx vertx, JdbcConnectionInfo jdbcConnectionInfo) {
    return new JdbcPoolCs(vertx, jdbcConnectionInfo);
  }


  /**
   * This function is called in Vertx verticle launch
   * <p>
   * because when you execute the {@link #init()} on the main event loop vertx,
   * you will get a report because it may take too much time
   * <p>
   * With it being wrapped in {@link Handler}
   * we can call it in {@link Vertx#executeBlocking(Handler)}
   */
  @Override
  public void handle(Promise<PgPool> promise) {

    PgPool jdbcPool;
    try {
      jdbcPool = this.init();
    } catch (DbMigrationException e) {
      promise.fail(e);
      return;
    }
    promise.complete(jdbcPool);

  }

  /**
   * Database migration and jdbc pool creation
   * <p>
   * This function is called in test setup
   * where there is no async method
   * <p>
   * If you execute the ini on the main event loop vertx,
   * you will get a report because it may take several minute.
   * Because {@link JdbcPoolCs} is also a {@link Handler}
   * you can call it {@link Vertx#executeBlocking(Handler)}
   * to not have this message
   */
  public PgPool init() throws DbMigrationException {

    try {
      return getJdbcPool(this.vertx);
    } catch (InternalException e) {
      // ok, not found
    }

    PGSimpleDataSource source = new PGSimpleDataSource();
    source.setURL(this.jdbcConnectionInfo.getUrl());
    String user = this.jdbcConnectionInfo.getUser();
    source.setUser(user);
    String password = this.jdbcConnectionInfo.getPassword();
    source.setPassword(password);

    /**
     * Init
     */
    JdbcSchemaManager.create(vertx, source)
      .setConnectionInfo(this.jdbcConnectionInfo)
      .migrateComboRealms()
      .migrateComboIp();


    // Set the working schema
    // https://vertx.io/docs/vertx-pg-client/java/#_data_object
    // https://www.postgresql.org/docs/current/runtime-config-client.html
    Map<String, String> connectionProps = new HashMap<>();
    connectionProps.put("search_path", this.jdbcConnectionInfo.getSchemaPath());

    String postgresUri = this.jdbcConnectionInfo.getPostgresUri();
    PgConnectOptions pgConnectOptions = PgConnectOptions.fromUri(postgresUri)
      .setUser(user)
      .setPassword(this.jdbcConnectionInfo.getPassword())
      .setProperties(connectionProps);

    PoolOptions poolOptions = new PoolOptions().setMaxSize(this.jdbcConnectionInfo.getMaxPoolSize());

    /**
     * Then create the pool and return
     */
    PgPool pool = PgPool.pool(
      this.vertx,
      pgConnectOptions,
      poolOptions
    );
    JdbcPoolCs.jdbcMap.put(this.vertx, pool);


    return pool;

  }


}
