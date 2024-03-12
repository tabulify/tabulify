package net.bytle.vertx;

import io.vertx.sqlclient.Pool;

import javax.sql.DataSource;

/**
 * An object that:
 * * wraps a client (ie pool)
 * * and expands it with a schema manager and the connection information
 */
public abstract class JdbcClient extends TowerService {

  public JdbcClient(Server server) {
    super(server);
  }


  abstract public JdbcSchemaManager getSchemaManager();

  abstract public Pool getPool();

  abstract public JdbcConnectionInfo getConnectionInfo();

  /**
   *
   * @return a data source to be used with third services such as Flyway with configuration
   */
  abstract public DataSource getDataSource();

}
