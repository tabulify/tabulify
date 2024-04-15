package net.bytle.vertx.db;

import io.vertx.core.Future;
import io.vertx.sqlclient.SqlConnection;
import io.vertx.sqlclient.Tuple;
import net.bytle.exception.InternalException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JdbcUpdate {


  private final JdbcTable jdbcTable;
  Map<JdbcTableColumn, Object> updatedColValues = new HashMap<>();
  Map<JdbcTableColumn, Object> primaryKeyColValues = new HashMap<>();
  private JdbcTableColumn returningColumn = null;

  private JdbcUpdate(JdbcTable jdbcTable) {
    this.jdbcTable = jdbcTable;

  }

  static public JdbcUpdate into(JdbcTable jdbcTable) {
    return new JdbcUpdate(jdbcTable);
  }

  public JdbcUpdate addUpdatedColumn(JdbcTableColumn cols, Object value) {
    this.updatedColValues.put(cols, value);
    return this;
  }

  public JdbcUpdate addPrimaryKeyColumn(JdbcTableColumn cols, Object value) {
    this.primaryKeyColValues.put(cols, value);
    return this;
  }

  public Future<JdbcRowSet> execute() {
    return this.jdbcTable.getSchema().getJdbcClient().getPool().getConnection()
      .compose(
        this::execute,
        err -> Future.failedFuture(new InternalException("JdbcUpdate: Unable to get a connection", err))
      );
  }

  public Future<JdbcRowSet> execute(SqlConnection sqlConnection) {
    StringBuilder updateSqlBuilder = new StringBuilder();
    List<Object> tuples = new ArrayList<>();
    updateSqlBuilder.append("update ")
      .append(this.jdbcTable.getFullName())
      .append(" set ")
    ;
    List<String> setColStatements = new ArrayList<>();
    for (Map.Entry<JdbcTableColumn, Object> entry : updatedColValues.entrySet()) {
      JdbcTableColumn jdbcTableColumn = entry.getKey();
      if (this.jdbcTable.getPrimaryKeyColumns().contains(jdbcTableColumn)) {
        return Future.failedFuture(new InternalException(this.jdbcTable.getFullName() + " update: column (" + jdbcTableColumn + ") is a primary key column and should not be updated"));
      }
      tuples.add(entry.getValue());
      setColStatements.add(jdbcTableColumn.getColumnName() + " = $" + tuples.size());

    }
    updateSqlBuilder.append(String.join(", ", setColStatements))
      .append(" where ");

    if (primaryKeyColValues.isEmpty()) {
      return Future.failedFuture(new InternalException(this.jdbcTable.getFullName() + " update miss the primary key columns"));
    }

    List<String> equalityStatements = new ArrayList<>();
    for (Map.Entry<JdbcTableColumn, Object> entry : primaryKeyColValues.entrySet()) {
      JdbcTableColumn jdbcTableColumn = entry.getKey();
      if (!this.jdbcTable.getPrimaryKeyColumns().contains(jdbcTableColumn)) {
        return Future.failedFuture(new InternalException(this.jdbcTable.getFullName() + " update: column (" + jdbcTableColumn + ") is not a declared primary key columns"));
      }
      tuples.add(entry.getValue());
      equalityStatements.add(jdbcTableColumn.getColumnName() + " = $" + tuples.size());

    }
    updateSqlBuilder.append(String.join(" and ", equalityStatements));

    if(this.returningColumn!=null){
      updateSqlBuilder.append(" returning ")
        .append(this.returningColumn.getColumnName());
    }

    String insertSqlString = updateSqlBuilder.toString();
    return sqlConnection
      .preparedQuery(insertSqlString)
      .execute(Tuple.from(tuples))
      .recover(e -> Future.failedFuture(new InternalException(this.jdbcTable.getFullName() + " table update Error. Sql Error " + e.getMessage() + "\nSQl: " + insertSqlString, e)))
      .compose(rowSet -> Future.succeededFuture(new JdbcRowSet(rowSet)));
  }

  public boolean hasNoColumnToUpdate() {
    return this.updatedColValues.isEmpty();
  }

  public JdbcUpdate addReturningColumn(JdbcTableColumn column) {
    this.returningColumn = column;
    return this;
  }
}
