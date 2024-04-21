package net.bytle.vertx.db;

import io.vertx.core.Future;
import io.vertx.sqlclient.SqlConnection;
import io.vertx.sqlclient.Tuple;
import net.bytle.exception.InternalException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JdbcInsert extends JdbcQuery {


  Map<JdbcColumn, Object> colValues = new HashMap<>();
  private JdbcColumn returningColumn = null;

  private JdbcInsert(JdbcTable jdbcTable) {
      super(jdbcTable);


  }

  static public JdbcInsert into(JdbcTable jdbcTable) {
    return new JdbcInsert(jdbcTable);
  }

  public JdbcInsert addColumn(JdbcColumn jdbcColumn, Object value) {
    if (this.colValues.containsKey(jdbcColumn)) {
      throw new InternalException("The column (" + jdbcColumn + ") is already present in the insertion list");
    }
    this.colValues.put(jdbcColumn, value);
    return this;
  }


  public Future<JdbcRowSet> execute(SqlConnection sqlConnection) {

    StringBuilder insertSqlBuilder = new StringBuilder();
    List<Object> tuples = new ArrayList<>();
    insertSqlBuilder.append("insert into ")
      .append(this.getDomesticJdbcTable().getFullName())
      .append(" (");

    List<String> colsStatement = new ArrayList<>();
    List<String> dollarStatement = new ArrayList<>();
    for (Map.Entry<JdbcColumn, Object> entry : colValues.entrySet()) {

      tuples.add(entry.getValue());
      colsStatement.add(entry.getKey().getColumnName());
      dollarStatement.add("$" + tuples.size());

    }
    insertSqlBuilder.append(String.join(",\n", colsStatement))
      .append(") values (")
      .append(String.join(",", dollarStatement))
      .append(")");

    if(this.returningColumn!=null){
      insertSqlBuilder
        .append(" returning ")
        .append(this.returningColumn.getColumnName());
    }

    String insertSqlString = insertSqlBuilder.toString();
    return sqlConnection
      .preparedQuery(insertSqlString)
      .execute(Tuple.from(tuples))
      .recover(e -> Future.failedFuture(new InternalException(this.getDomesticJdbcTable().getFullName() + " table insertion Error. Sql Error " + e.getMessage() + "\nSQl: " + insertSqlString, e)))
      .compose(rowSet -> Future.succeededFuture(new JdbcRowSet(rowSet)));
  }


  public JdbcInsert addReturningColumn(JdbcColumn jdbcCol) {
    this.returningColumn  = jdbcCol;
    return this;
  }
}
