package net.bytle.vertx.db;

import io.vertx.core.Future;
import io.vertx.sqlclient.SqlConnection;
import net.bytle.exception.InternalException;

public abstract class JdbcQuery {

  private final JdbcTable jdbcTable;
  public JdbcQuery(JdbcTable jdbcTable) {
    this.jdbcTable = jdbcTable;
  }

  public JdbcTable getJdbcTable() {
    return jdbcTable;
  }

  public Future<JdbcRowSet> execute() {
    return this.getJdbcTable().getSchema().getJdbcClient().getPool().getConnection()
      .compose(
        this::execute,
        err -> Future.failedFuture(new InternalException(this.getClass().getSimpleName() +": Unable to get a connection", err))
      );
  }

  public abstract Future<JdbcRowSet> execute(SqlConnection sqlConnection);

}
