package net.bytle.vertx;

import io.vertx.sqlclient.Pool;

/**
 * An object that:
 * * wraps a client (ie pool)
 * * and augment it with a schema manager and the connection information
 */
public interface JdbcClient extends TowerServiceInterface {

  JdbcSchemaManager getSchemaManager();

  Pool getPool();

  JdbcConnectionInfo getConnectionInfo();

}
