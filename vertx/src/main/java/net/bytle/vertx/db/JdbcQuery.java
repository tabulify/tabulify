package net.bytle.vertx.db;

import io.vertx.core.Future;
import io.vertx.sqlclient.SqlConnection;
import net.bytle.exception.InternalException;

import java.util.function.Function;

public abstract class JdbcQuery {

  private final JdbcTable jdbcTable;

  public JdbcQuery(JdbcTable jdbcTable) {
    this.jdbcTable = jdbcTable;
  }

  public JdbcTable getDomesticJdbcTable() {
    return jdbcTable;
  }

  public Future<JdbcRowSet> execute() {
    return this.getDomesticJdbcTable().getSchema().getJdbcClient().getPool()
      .getConnection()
      .compose(
        connection -> this.execute(connection)
          .eventually(() -> connection.close()),
        err -> Future.failedFuture(new InternalException(this.getClass().getSimpleName() + ": Unable to get a connection", err))
      );
  }

  public <T> Future<T> execute(Function<JdbcRowSet, Future<T>> buildFunction) {
    return this.getDomesticJdbcTable().getSchema().getJdbcClient().getPool()
      .getConnection()
      .recover(err -> Future.failedFuture(new InternalException(this.getClass().getSimpleName() + ": Unable to get a connection", err)))
      .compose(
        connection -> this.execute(connection)
          .compose(buildFunction)
          .recover(err -> Future.failedFuture(new InternalException(this.getClass().getSimpleName() + ": The build function returns an error. Error: "+err.getMessage(), err)))
          .eventually(() -> connection.close())
      );
  }

  public <T> Future<T> execute(SqlConnection sqlConnection, Function<JdbcRowSet, Future<T>> buildFunction) {
    return this.execute(sqlConnection)
      .compose(buildFunction)
      .recover(err -> Future.failedFuture(new InternalException(this.getClass().getSimpleName() + ": The build function errors", err)));
  }

  public abstract Future<JdbcRowSet> execute(SqlConnection sqlConnection);

}
