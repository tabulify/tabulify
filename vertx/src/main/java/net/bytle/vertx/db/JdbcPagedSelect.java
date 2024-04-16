package net.bytle.vertx.db;

import io.vertx.core.Future;
import io.vertx.sqlclient.SqlConnection;
import io.vertx.sqlclient.Tuple;
import net.bytle.exception.InternalException;
import org.jetbrains.annotations.NotNull;

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

    if (this.innerJoinTables.size() != 1) {
      return Future.failedFuture(new InternalException("The Paged Select supports for now a SQL with 2 tables, only one declared."));
    }
    JdbcTable joinedTable = this.innerJoinTables.iterator().next();

    List<Object> tuples = new ArrayList<>();

    /**
     * The SQL block with the row_number and the data
     * (It will be enclosed by the pagination filtering)
     */
    StringBuilder sqlDataBlock = new StringBuilder();
    /**
     * The select
     */
    sqlDataBlock
      .append("select ")
      .append(String.join(", ", getSelectColumnsSqlExpressions()))
      .append(" from ")
      .append(this.getJdbcTable().getFullName()).append(" ").append(this.getJdbcTable().getName())
      .append(" inner join ").append(joinedTable.getFullName()).append(" ").append(joinedTable.getName());

    /**
     * On columns
     */
    List<String> onSqlColumnPredicate = new ArrayList<>();
    Map<JdbcTableColumn, JdbcTableColumn> foreignKeyColumnMapping = this.getJdbcTable().getForeignKeyColumns(joinedTable);
    if (foreignKeyColumnMapping.isEmpty()) {
      return Future.failedFuture(new InternalException("The foreign key column mapping for the table (" + joinedTable + ") are not present in the table definition of (" + this.getJdbcTable() + ")"));
    }
    for (Map.Entry<JdbcTableColumn, JdbcTableColumn> joinColumnMapping : foreignKeyColumnMapping.entrySet()) {
      onSqlColumnPredicate.add(this.getJdbcTable().getName() + "." + joinColumnMapping.getKey().getColumnName() + " = " + joinedTable.getName() + "." + joinColumnMapping.getValue().getColumnName());
    }
    sqlDataBlock
      .append(" on ")
      .append(String.join(" and ", onSqlColumnPredicate))
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
    sqlDataBlock.append(String.join(" and ", predicateStatements));

    if (this.limit != null) {
      sqlDataBlock.append(" LIMIT ").append(this.limit);
    }

    String insertSqlString = sqlDataBlock.toString();
    return sqlConnection
      .preparedQuery(insertSqlString)
      .execute(Tuple.from(tuples))
      .recover(e -> Future.failedFuture(new InternalException(this.getJdbcTable().getFullName() + " table select Error. Sql Error " + e.getMessage() + "\nSQl: " + insertSqlString, e)))
      .compose(rowSet -> Future.succeededFuture(new JdbcRowSet(rowSet)));
  }

  @NotNull
  private List<String> getSelectColumnsSqlExpressions() {
    List<String> selectColumnsSqlExpressions = new ArrayList<>();
    selectColumnsSqlExpressions.add("ROW_NUMBER() OVER (ORDER BY " + this.orderedByTable.getName() + "." + this.orderedByColumn.getColumnName() + " " + this.orderBySort + ")");
    selectColumnsSqlExpressions.add(this.getJdbcTable().getName() + ".* ");
    for (Map.Entry<JdbcTable, Map<JdbcTableColumn, String>> selectTable : this.selectedColumns.entrySet()) {
      for (Map.Entry<JdbcTableColumn, String> selectColumn : selectTable.getValue().entrySet()) {
        selectColumnsSqlExpressions.add(selectTable.getKey().getName() + "." + selectColumn.getKey().getColumnName() + " as " + selectColumn.getValue());
      }
    }
    return selectColumnsSqlExpressions;
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
    Map<JdbcTableColumn, String> tableColumns = this.selectedColumns.computeIfAbsent(userTable, key -> new HashMap<>());
    tableColumns.put(column, column.getColumnName());
    return this;
  }
}
