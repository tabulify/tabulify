package net.bytle.vertx.db;

import io.vertx.core.Future;
import io.vertx.sqlclient.SqlConnection;
import io.vertx.sqlclient.Tuple;
import net.bytle.exception.InternalException;

import java.util.ArrayList;
import java.util.List;

public class JdbcSelect extends JdbcQuery {


  List<JdbcSingleOperatorPredicate> predicateColValues = new ArrayList<>();
  private Long limit = null;

  private JdbcSelect(JdbcTable jdbcTable) {
    super(jdbcTable);
  }

  static public JdbcSelect from(JdbcTable jdbcTable) {
    return new JdbcSelect(jdbcTable);
  }


  public JdbcSelect addPredicate(JdbcSingleOperatorPredicate predicate) {
    this.predicateColValues.add(predicate);
    return this;
  }

  public JdbcSelect addEqualityPredicate(JdbcTableColumn cols, Object value) {
    this.predicateColValues.add(JdbcSingleOperatorPredicate.builder()
      .setColumn(cols, value)
      .build());
    return this;
  }


  public Future<JdbcRowSet> execute(SqlConnection sqlConnection) {

    if (predicateColValues.isEmpty()) {
      return Future.failedFuture(new InternalException(this.getJdbcTable().getFullName() + " select has no predicates"));
    }

    StringBuilder selectSqlBuilder = new StringBuilder();
    List<Object> tuples = new ArrayList<>();
    selectSqlBuilder.append("select * from ")
      .append(this.getJdbcTable().getFullName())
      .append(" where ");


    List<String> predicateStatements = new ArrayList<>();
    for (JdbcSingleOperatorPredicate predicate : predicateColValues) {

      tuples.add(predicate.getValue());
      StringBuilder predicateBuilder = new StringBuilder();
      if (predicate.getOrNull()) {
        predicateBuilder.append("(");
      }
      predicateBuilder
        .append(predicate.getColumn().getColumnName())
        .append(" ")
        .append(predicate.getComparisonOperator().toSql())
        .append(" $")
        .append(tuples.size());
      if (predicate.getOrNull()) {
        predicateBuilder
          .append(" or ")
          .append(predicate.getColumn().getColumnName())
          .append(" is null)");
      }
      predicateStatements.add(predicateBuilder.toString());
    }
    selectSqlBuilder.append(String.join(" and ", predicateStatements));

    if (this.limit != null) {
      selectSqlBuilder.append(" LIMIT ").append(this.limit);
    }

    String insertSqlString = selectSqlBuilder.toString();
    return sqlConnection
      .preparedQuery(insertSqlString)
      .execute(Tuple.from(tuples))
      .recover(e -> Future.failedFuture(new InternalException(this.getJdbcTable().getFullName() + " table select Error. Sql Error " + e.getMessage() + "\nSQl: " + insertSqlString, e)))
      .compose(rowSet -> Future.succeededFuture(new JdbcRowSet(rowSet)));
  }

  public JdbcSelect addLimit(Long limit) {
    this.limit = limit;
    return this;
  }
}
