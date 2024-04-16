package net.bytle.vertx.db;

import io.vertx.core.Future;
import io.vertx.sqlclient.SqlConnection;
import io.vertx.sqlclient.Tuple;
import net.bytle.exception.InternalException;

import java.util.*;

/**
 * A utility to create paged query
 */
public class JdbcPagedSelect extends JdbcQuery {


  List<JdbcSingleOperatorPredicate> predicateColValues = new ArrayList<>();
  private Long limit = null;
  private Set<JdbcTable> innerJoinTables = new HashSet<>();
  private JdbcTable searchTable;
  private JdbcTableColumn searchColumn;
  private JdbcTableColumn orderedByColumn;
  private JdbcTable orderedByTable;
  private JdbcSort orderBySort;
  private JdbcPagination pagination;
  /**
   * By table, the column and their alias
   */
  private Map<JdbcTable, Map<JdbcTableColumn, String>> selectedColumns = new HashMap<>();

  private JdbcPagedSelect(JdbcTable jdbcTable) {
    super(jdbcTable);
  }

  static public JdbcPagedSelect from(JdbcTable jdbcTable) {
    return new JdbcPagedSelect(jdbcTable);
  }


  public JdbcPagedSelect addPredicate(JdbcSingleOperatorPredicate predicate) {
    this.predicateColValues.add(predicate);
    return this;
  }

  public JdbcPagedSelect addEqualityPredicate(JdbcTable userTable, JdbcTableColumn cols, Object value) {
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


  public JdbcPagedSelect setSearchColumn(JdbcTable searchTable, JdbcTableColumn searchColumn) {
    this.innerJoinTables.add(searchTable);
    this.searchTable = searchTable;
    this.searchColumn = searchColumn;
    return this;
  }

  public JdbcPagedSelect addOrderBy(JdbcTable orderedByTable, JdbcTableColumn orderedByCol, JdbcSort jdbcSort) {
    this.innerJoinTables.add(orderedByTable);
    this.orderedByTable = orderedByTable;
    this.orderedByColumn = orderedByCol;
    this.orderBySort = jdbcSort;
    return this;
  }


  public JdbcPagedSelect setPagination(JdbcPagination pagination) {
    this.pagination = pagination;
    return this;
  }

  public JdbcPagedSelect addExtraSelectColumn(JdbcTable userTable, JdbcTableColumn column) {
    this.innerJoinTables.add(userTable);
    Map<JdbcTableColumn, String> tableColumns = this.selectedColumns.computeIfAbsent(userTable, key->new HashMap<>() );
    tableColumns.put(column,column.getColumnName());
    return this;
  }
}
