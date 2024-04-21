package net.bytle.vertx.db;

import io.vertx.sqlclient.Pool;
import net.bytle.exception.InternalException;
import net.bytle.type.Strings;
import net.bytle.vertx.Server;
import net.bytle.vertx.TowerService;

import javax.sql.DataSource;

/**
 * An object that:
 * * wraps a client (ie pool)
 * * and expands it with:
 *   * a schema manager
 *   * connection information
 *   * read sql in resources
 *   * database info (with {@link JdbcSqlStatementEngine})
 */
public abstract class JdbcClient extends TowerService {

  private final JdbcSqlStatementEngine databaseMeta = new JdbcSqlStatementEngine();

  public JdbcClient(Server server) {
    super(server);
  }


  abstract public JdbcSchemaManager getSchemaManager();

  abstract public Pool getPool();

  /**
   *
   * @return the database info that we have declared
   */
  abstract public JdbcConnectionInfo getConnectionInfo();

  /**
   *
   * @return a data source to be used with third services such as Flyway with configuration
   */
  abstract public DataSource getDataSource();

  /**
   * Utility function to get a sql from a file
   * in the resource
   */
  public String getSqlStatement(String fileName) {

    String path = "/db/parameterized-statement/" + fileName;
    String sql = Strings.createFromResource(JdbcClient.class, path).toString();
    if (sql == null) {
      throw new InternalException("The sql (" + fileName + ") was not found in the resource path (" + path + ")");
    }
    return sql;

  }

  public JdbcSqlStatementEngine getSqlStatementEngine() {
    return this.databaseMeta;
  }


}
