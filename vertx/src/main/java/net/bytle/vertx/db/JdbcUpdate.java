package net.bytle.vertx.db;

import io.vertx.core.Future;
import io.vertx.sqlclient.SqlConnection;
import io.vertx.sqlclient.Tuple;
import net.bytle.exception.InternalException;

import java.util.*;
import java.util.stream.Collectors;

public class JdbcUpdate extends JdbcQuery {

  Map<JdbcColumn, Object> updatedColValues = new HashMap<>();
  Map<JdbcColumn, Object> predicateColValues = new HashMap<>();
  private JdbcColumn returningColumn = null;

  private JdbcUpdate(JdbcTable jdbcTable) {
      super(jdbcTable);

  }

  static public JdbcUpdate into(JdbcTable jdbcTable) {
    return new JdbcUpdate(jdbcTable);
  }

  public JdbcUpdate addUpdatedColumn(JdbcColumn cols, Object value) {
    this.updatedColValues.put(cols, value);
    return this;
  }

  public JdbcUpdate addPredicateColumn(JdbcColumn cols, Object value) {
    this.predicateColValues.put(cols, value);
    return this;
  }

  public Future<JdbcRowSet> execute(SqlConnection sqlConnection) {
    JdbcPreparedStatement preparedStatement = this.toPreparedStatement();
    String preparedSql = preparedStatement.getPreparedSql();
    List<Object> bindingValues = preparedStatement.getBindingValues();
    return sqlConnection
      .preparedQuery(preparedSql)
      .execute(Tuple.from(bindingValues))
      .recover(e -> Future.failedFuture(new InternalException(this.getDomesticJdbcTable().getFullName() + " table update Error. Sql Error " + e.getMessage() + "\nValues:" + bindingValues.stream().map(Objects::toString).collect(Collectors.joining(", ")) + "\nSQl: " + preparedSql, e)))
      .compose(rowSet -> Future.succeededFuture(new JdbcRowSet(rowSet)));
  }

  @Override
  public JdbcPreparedStatement toPreparedStatement() {
    StringBuilder updateSqlBuilder = new StringBuilder();
    List<Object> bindingValues = new ArrayList<>();
    updateSqlBuilder.append("update ")
      .append(this.getDomesticJdbcTable().getFullName())
      .append(" set ")
    ;
    List<String> setColStatements = new ArrayList<>();
    for (Map.Entry<JdbcColumn, Object> entry : updatedColValues.entrySet()) {
      JdbcColumn jdbcColumn = entry.getKey();
      if (this.getDomesticJdbcTable().getPrimaryKeyColumns().contains(jdbcColumn)) {
        throw new InternalException(this.getDomesticJdbcTable().getFullName() + " update: column (" + jdbcColumn + ") is a primary key column and should not be updated");
      }
      bindingValues.add(entry.getValue());
      setColStatements.add(jdbcColumn.getColumnName() + " = $" + bindingValues.size());

    }
    updateSqlBuilder.append(String.join(", ", setColStatements))
      .append(" where ");

    if (predicateColValues.isEmpty()) {
      throw new InternalException(this.getDomesticJdbcTable().getFullName() + " update miss the primary key columns");
    }

    List<String> equalityStatements = new ArrayList<>();
    for (Map.Entry<JdbcColumn, Object> entry : predicateColValues.entrySet()) {
      JdbcColumn jdbcColumn = entry.getKey();
      if (!this.getDomesticJdbcTable().getPrimaryKeyColumns().contains(jdbcColumn)) {
        throw new InternalException(this.getDomesticJdbcTable().getFullName() + " update: column (" + jdbcColumn + ") is not a declared primary or unique key columns for the table ("+this.getDomesticJdbcTable()+")");
      }
      bindingValues.add(entry.getValue());
      equalityStatements.add(jdbcColumn.getColumnName() + " = $" + bindingValues.size());
    }
    updateSqlBuilder.append(String.join(" and ", equalityStatements));

    if(this.returningColumn!=null){
      updateSqlBuilder.append(" returning ")
        .append(this.returningColumn.getColumnName());
    }

    String preparedStatement = updateSqlBuilder.toString();
    return new JdbcPreparedStatement(preparedStatement,bindingValues);
  }

  public boolean hasNoColumnToUpdate() {
    return this.updatedColValues.isEmpty();
  }

  public JdbcUpdate addReturningColumn(JdbcColumn column) {
    // https://vertx.io/docs/vertx-pg-client/java/#_returning_clauses
    this.returningColumn = column;
    return this;
  }
}
