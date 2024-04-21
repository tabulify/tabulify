package net.bytle.vertx.db;

import io.vertx.core.Future;
import io.vertx.sqlclient.SqlConnection;
import io.vertx.sqlclient.Tuple;
import net.bytle.exception.InternalException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    StringBuilder updateSqlBuilder = new StringBuilder();
    List<Object> tuples = new ArrayList<>();
    updateSqlBuilder.append("update ")
      .append(this.getDomesticJdbcTable().getFullName())
      .append(" set ")
    ;
    List<String> setColStatements = new ArrayList<>();
    for (Map.Entry<JdbcColumn, Object> entry : updatedColValues.entrySet()) {
      JdbcColumn jdbcColumn = entry.getKey();
      if (this.getDomesticJdbcTable().getPrimaryOrUniqueKeyColumns().contains(jdbcColumn)) {
        return Future.failedFuture(new InternalException(this.getDomesticJdbcTable().getFullName() + " update: column (" + jdbcColumn + ") is a primary key column and should not be updated"));
      }
      tuples.add(entry.getValue());
      setColStatements.add(jdbcColumn.getColumnName() + " = $" + tuples.size());

    }
    updateSqlBuilder.append(String.join(", ", setColStatements))
      .append(" where ");

    if (predicateColValues.isEmpty()) {
      return Future.failedFuture(new InternalException(this.getDomesticJdbcTable().getFullName() + " update miss the primary key columns"));
    }

    List<String> equalityStatements = new ArrayList<>();
    for (Map.Entry<JdbcColumn, Object> entry : predicateColValues.entrySet()) {
      JdbcColumn jdbcColumn = entry.getKey();
      if (!this.getDomesticJdbcTable().getPrimaryOrUniqueKeyColumns().contains(jdbcColumn)) {
        return Future.failedFuture(new InternalException(this.getDomesticJdbcTable().getFullName() + " update: column (" + jdbcColumn + ") is not a declared primary or unique key columns for the table ("+this.getDomesticJdbcTable()+")"));
      }
      tuples.add(entry.getValue());
      equalityStatements.add(jdbcColumn.getColumnName() + " = $" + tuples.size());

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
      .recover(e -> Future.failedFuture(new InternalException(this.getDomesticJdbcTable().getFullName() + " table update Error. Sql Error " + e.getMessage() + "\nSQl: " + insertSqlString, e)))
      .compose(rowSet -> Future.succeededFuture(new JdbcRowSet(rowSet)));
  }

  public boolean hasNoColumnToUpdate() {
    return this.updatedColValues.isEmpty();
  }

  public JdbcUpdate addReturningColumn(JdbcColumn column) {
    this.returningColumn = column;
    return this;
  }
}
