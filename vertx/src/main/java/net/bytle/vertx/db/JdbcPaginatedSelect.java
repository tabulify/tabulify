package net.bytle.vertx.db;

import io.vertx.core.Future;
import io.vertx.sqlclient.SqlConnection;
import io.vertx.sqlclient.Tuple;
import net.bytle.exception.InternalException;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * A utility to create paginated query
 */
public class JdbcPaginatedSelect extends JdbcQuery {


  List<JdbcSingleOperatorPredicate> predicateColValues = new ArrayList<>();

  private final Set<JdbcTable> innerJoinTables = new HashSet<>();
  private JdbcTable searchTable;
  private JdbcTableColumn searchColumn;
  private JdbcTableColumn orderedByColumn;
  private JdbcTable orderedByTable;
  private JdbcSort orderBySort;
  private JdbcPagination pagination;
  /**
   * By table, the column and their alias
   */
  private final Map<JdbcTable, Map<JdbcTableColumn, String>> selectedColumns = new HashMap<>();

  private JdbcPaginatedSelect(JdbcTable jdbcTable) {
    super(jdbcTable);
  }

  static public JdbcPaginatedSelect from(JdbcTable jdbcTable) {
    return new JdbcPaginatedSelect(jdbcTable);
  }


  public JdbcPaginatedSelect addEqualityPredicate(JdbcTable userTable, JdbcTableColumn cols, Object value) {
    this.predicateColValues.add(JdbcSingleOperatorPredicate.builder()
      .setColumn(userTable, cols, value)
      .build());
    return this;
  }


  public Future<JdbcRowSet> execute(SqlConnection sqlConnection) {

    if (this.innerJoinTables.size() != 1) {
      return Future.failedFuture(new InternalException("The Paged Select supports for now a SQL with 2 tables, only one declared."));
    }
    JdbcTable joinedTable = this.innerJoinTables.iterator().next();


    /**
     * The SQL block with the row_number and the data
     * (It will be enclosed by the pagination filtering)
     */
    StringBuilder sqlDataBlock = new StringBuilder();
    String orderByColumnAlias = "rowNumber";
    String orderBySql = "ORDER BY " + this.orderedByTable.getName() + "." + this.orderedByColumn.getColumnName() + " " + this.orderBySort;
    /**
     * The select
     */
    sqlDataBlock
      .append("select ")
      .append(String.join(", ", getSelectColumnsSqlExpressions(orderBySql, orderByColumnAlias)))
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

    List<Object> bindingValues = new ArrayList<>();
    List<String> predicateStatements = new ArrayList<>();
    for (JdbcSingleOperatorPredicate predicate : predicateColValues) {
      bindingValues.add(predicate.getValue());
      predicateStatements.add(predicate.toSql(bindingValues.size()));
    }
    String searchTerm = this.pagination.getSearchTerm();
    if (!searchTerm.isEmpty()) {
      bindingValues.add("%" + searchTerm + "%");
      predicateStatements.add(this.searchTable.getName() + "." + this.searchColumn.getColumnName() + " like $" + bindingValues.size());
    }
    sqlDataBlock
      .append(String.join(" and ", predicateStatements));

    String sqlDataBlockString = sqlDataBlock.toString();

    /**
     * Pagination block
     */
    StringBuilder finalSqlQuery = new StringBuilder();
    String queryDataBlockName = "pagination";
    finalSqlQuery
      .append("select ")
      .append(queryDataBlockName)
      .append(".* from (")
      .append(sqlDataBlockString)
      .append(") ")
      .append(queryDataBlockName)
      .append(" where ");

    // Greater than (if 0, then 1)
    bindingValues.add(pagination.getPageId() * pagination.getPageSize());
    finalSqlQuery.append(orderByColumnAlias)
      .append(" >= 1 + $")
      .append(bindingValues.size());

    // Less than (if 0, then page size)
    bindingValues.add((pagination.getPageId() + 1) * pagination.getPageSize());
    finalSqlQuery
      .append(" and ")
      .append(orderByColumnAlias)
      .append(" < 1 + $")
      .append(bindingValues.size());

    // final order by
    finalSqlQuery.append(" ")
      .append(" ORDER BY ")
      .append(queryDataBlockName)
      .append(".")
      .append(this.orderedByColumn.getColumnName())
      .append(" ")
      .append(this.orderBySort);

    return sqlConnection
      .preparedQuery(finalSqlQuery.toString())
      .execute(Tuple.from(bindingValues))
      .recover(e -> Future.failedFuture(new InternalException(this.getJdbcTable().getFullName() + " table paginated select Error. Sql Error " + e.getMessage() + "\nSQl: " + finalSqlQuery, e)))
      .compose(rowSet -> Future.succeededFuture(new JdbcRowSet(rowSet)));
  }

  @NotNull
  private List<String> getSelectColumnsSqlExpressions(String orderBy, String orderByColumnAlias) {
    List<String> selectColumnsSqlExpressions = new ArrayList<>();

    selectColumnsSqlExpressions.add("ROW_NUMBER() OVER (" + orderBy + ") " + orderByColumnAlias);
    selectColumnsSqlExpressions.add(this.getJdbcTable().getName() + ".* ");
    for (Map.Entry<JdbcTable, Map<JdbcTableColumn, String>> selectTable : this.selectedColumns.entrySet()) {
      for (Map.Entry<JdbcTableColumn, String> selectColumn : selectTable.getValue().entrySet()) {
        selectColumnsSqlExpressions.add(selectTable.getKey().getName() + "." + selectColumn.getKey().getColumnName() + " as " + selectColumn.getValue());
      }
    }
    return selectColumnsSqlExpressions;
  }


  public JdbcPaginatedSelect setSearchColumn(JdbcTable searchTable, JdbcTableColumn searchColumn) {
    this.innerJoinTables.add(searchTable);
    this.searchTable = searchTable;
    this.searchColumn = searchColumn;
    return this;
  }

  public JdbcPaginatedSelect addOrderBy(JdbcTable orderedByTable, JdbcTableColumn orderedByCol, JdbcSort jdbcSort) {
    this.innerJoinTables.add(orderedByTable);
    this.orderedByTable = orderedByTable;
    this.orderedByColumn = orderedByCol;
    this.orderBySort = jdbcSort;
    return this;
  }


  public JdbcPaginatedSelect setPagination(JdbcPagination pagination) {
    this.pagination = pagination;
    return this;
  }

  public JdbcPaginatedSelect addExtraSelectColumn(JdbcTable userTable, JdbcTableColumn column) {
    this.innerJoinTables.add(userTable);
    Map<JdbcTableColumn, String> tableColumns = this.selectedColumns.computeIfAbsent(userTable, key -> new HashMap<>());
    tableColumns.put(column, column.getColumnName());
    return this;
  }

}
