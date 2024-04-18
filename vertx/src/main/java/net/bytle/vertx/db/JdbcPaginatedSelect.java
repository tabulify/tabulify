package net.bytle.vertx.db;

import io.vertx.core.Future;
import io.vertx.sqlclient.SqlConnection;
import io.vertx.sqlclient.Tuple;
import net.bytle.exception.InternalException;

import java.util.List;

/**
 * A utility to create a paginated query.
 * It wraps a {@link JdbcSelect} by adding a {@link JdbcPaginatedSelect#setPagination(JdbcPagination) pagination}
 * and a {@link JdbcPaginatedSelect#addOrderBy(JdbcTableColumn) orderBy}
 */
public class JdbcPaginatedSelect extends JdbcQuery {


  /**
   * The SQL block with the row_number and the data
   * (It will be enclosed by the pagination filtering)
   */
  private final JdbcSelect mainSelect;

  private JdbcTable searchTable;
  private JdbcTableColumn searchColumn;
  private JdbcTableColumn orderedByColumn;
  private JdbcTable orderedByTable;
  private JdbcSort orderBySort;
  private JdbcPagination pagination;


  private JdbcPaginatedSelect(JdbcTable jdbcTable) {
    super(jdbcTable);
    this.mainSelect = JdbcSelect.from(this.getJdbcTable());
  }

  static public JdbcPaginatedSelect from(JdbcTable jdbcTable) {
    return new JdbcPaginatedSelect(jdbcTable);
  }


  public JdbcPaginatedSelect addEqualityPredicate(JdbcTable userTable, JdbcTableColumn cols, Object value) {
    this.mainSelect.addEqualityPredicate(userTable, cols, value);
    return this;
  }


  public Future<JdbcRowSet> execute(SqlConnection sqlConnection) {


    String orderByColumnAlias = "rowNumber";
    String orderBySql = "ORDER BY " + this.orderedByTable.getName() + "." + this.orderedByColumn.getColumnName() + " " + this.orderBySort;
    this.mainSelect.addSelectExpression("ROW_NUMBER() OVER (" + orderBySql + ") " + orderByColumnAlias);

    String searchTerm = this.pagination.getSearchTerm();
    if (!(searchTerm == null || searchTerm.isEmpty())) {
      this.mainSelect.addPredicate(
        JdbcSingleOperatorPredicate.builder()
          .setColumn(this.searchTable, this.searchColumn, "%" + searchTerm + "%")
          .setOperator(JdbcComparisonOperator.LIKE)
          .build()
      );
    }

    /**
     * Primary Sql Block
     */
    JdbcPreparedStatement mainPreparedStatement = this.mainSelect.toPreparedStatement();

    String mainSelectSql = mainPreparedStatement.getPreparedSql();
    List<Object> bindingValues = mainPreparedStatement.getBindingValues();
    /**
     * Pagination block
     */
    StringBuilder finalSqlQuery = new StringBuilder();
    String queryDataBlockName = "pagination";
    finalSqlQuery
      .append("select ")
      .append(queryDataBlockName)
      .append(".* from (")
      .append(mainSelectSql)
      .append(") ")
      .append(queryDataBlockName)
      .append(" where ");

    // Greater than (if 0, then 1)
    bindingValues.add((pagination.getPageId() - 1) * pagination.getPageSize());
    finalSqlQuery.append(orderByColumnAlias)
      .append(" >= 1 + $")
      .append(bindingValues.size());

    // Less than (if 0, then page size)
    bindingValues.add((pagination.getPageId()) * pagination.getPageSize());
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


  public JdbcPaginatedSelect setSearchColumn(JdbcTable searchTable, JdbcTableColumn searchColumn) {
    this.searchTable = searchTable;
    this.searchColumn = searchColumn;
    return this;
  }

  public JdbcPaginatedSelect addOrderBy(JdbcTable orderedByTable, JdbcTableColumn orderedByCol, JdbcSort jdbcSort) {
    /**
     * The orderBy happens at the end, we don't add it in the mainSelect
     */
    this.orderedByTable = orderedByTable;
    this.orderedByColumn = orderedByCol;
    this.orderBySort = jdbcSort;
    return this;
  }

  public JdbcPaginatedSelect addOrderBy(JdbcTableColumn orderedByCol) {
    addOrderBy(this.getJdbcTable(), orderedByCol, JdbcSort.ASC);
    return this;
  }


  public JdbcPaginatedSelect setPagination(JdbcPagination pagination) {
    this.pagination = pagination;
    Long pageId = pagination.getPageId();
    if (pageId <= 0) {
      throw new IllegalArgumentException("The pagination id value (" + pageId + ") is not greater than zero. Should start at 1");
    }
    return this;
  }

  public JdbcPaginatedSelect addExtraSelectColumn(JdbcTable jdbcTable, JdbcTableColumn column) {
    mainSelect.addExtraSelectColumn(jdbcTable, column);
    return this;
  }

  public JdbcPaginatedSelect addEqualityPredicate(JdbcTableColumn column, Object value) {
    mainSelect.addEqualityPredicate(this.getJdbcTable(), column, value);
    return this;
  }

  public JdbcPaginatedSelect setSearchColumn(JdbcTableColumn column) {
    this.setSearchColumn(this.getJdbcTable(), column);
    return this;
  }

}
