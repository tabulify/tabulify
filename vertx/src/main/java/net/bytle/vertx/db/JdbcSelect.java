package net.bytle.vertx.db;

import io.vertx.core.Future;
import io.vertx.sqlclient.SqlConnection;
import io.vertx.sqlclient.Tuple;
import net.bytle.exception.InternalException;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class JdbcSelect extends JdbcQuery {


  private final JdbcSqlStatementEngine sqlStatementEngine;
  List<JdbcSingleOperatorPredicate> predicateColValues = new ArrayList<>();
  private Long limit = null;

  /**
   * The select expression (The dev can add an expression with function such as count, row_number)
   */
  private final List<String> selectExpressions = new ArrayList<>();

  /**
   * The collected tables (without the main table)
   * (only inner join is supported)
   */
  private final Set<JdbcTable> foreignTables = new HashSet<>();

  /**
   * Extra Selected columns (that does not belong to the main table)
   * By table, the column and their alias
   */
  private final Map<JdbcColumn, String> foreignSelectedColumns = new HashMap<>();

  private JdbcSelect(JdbcTable jdbcTable) {

    super(jdbcTable);
    this.sqlStatementEngine = jdbcTable.getSchema().getJdbcClient().getSqlStatementEngine();

  }

  static public JdbcSelect from(JdbcTable jdbcTable) {
    return new JdbcSelect(jdbcTable);
  }


  public JdbcSelect addPredicate(JdbcSingleOperatorPredicate.Builder predicateBuilder) {

    this.predicateColValues.add(
      predicateBuilder
        .build(this.sqlStatementEngine)
    );
    return this;
  }

  public JdbcSelect addEqualityPredicate(JdbcColumn jdbcColumn, Object value) {
    this.addEventuallyForeignTable(jdbcColumn);

    this.predicateColValues.add(
      JdbcSingleOperatorPredicate.create()
        .setColumn(jdbcColumn, value)
        .build(sqlStatementEngine)
    );
    return this;
  }


  public Future<JdbcRowSet> execute(SqlConnection sqlConnection) {

    JdbcPreparedStatement preparedStatement = this.toPreparedStatement();
    return sqlConnection
      .preparedQuery(preparedStatement.getPreparedSql())
      .execute(Tuple.from(preparedStatement.getBindingValues()))
      .recover(e -> Future.failedFuture(new InternalException(this.getDomesticJdbcTable().getFullName() + " table select Error. Sql Error " + e.getMessage() + "\nSQl: " + preparedStatement, e)))
      .compose(rowSet -> Future.succeededFuture(new JdbcRowSet(rowSet)));
  }

  public JdbcSelect addLimit(Long limit) {
    this.limit = limit;
    return this;
  }

  @NotNull
  private List<String> getSelectColumnsSqlExpressions() {

    List<String> selectColumnsSqlExpressions = new ArrayList<>(this.selectExpressions);
    selectColumnsSqlExpressions.add(this.getDomesticJdbcTable().getName() + ".* ");
    for (Map.Entry<JdbcColumn, String> foreignSelectColumn : this.foreignSelectedColumns.entrySet()) {
      JdbcColumn foreignColumn = foreignSelectColumn.getKey();
      String alias = foreignSelectColumn.getValue();
      JdbcTable foreignTable = this.getDomesticJdbcTable().getSchema().getJdbcClient().getSqlStatementEngine().getTableOfColumn(foreignColumn);
      selectColumnsSqlExpressions.add(foreignTable.getName() + "." + foreignColumn.getColumnName() + " as " + alias);
    }
    return selectColumnsSqlExpressions;
  }

  public void addSelectExpression(String selectExpression) {
    this.selectExpressions.add(selectExpression);
  }


  /**
   *
   * @param jdbcColumn - a column to add that may be domestic or foreign
   * @return true if the column is a foreign column
   */
  private Boolean addEventuallyForeignTable(JdbcColumn jdbcColumn) {

    JdbcTable domesticJdbcTable = this.getDomesticJdbcTable();
    if (domesticJdbcTable.hasColumn(jdbcColumn)) {
      return false;
    }

    JdbcTable foreignTable = domesticJdbcTable.getSchema().getJdbcClient().getSqlStatementEngine().getTableOfColumn(jdbcColumn);
    this.foreignTables.add(foreignTable);
    return true;

  }

  @Override
  public JdbcPreparedStatement toPreparedStatement() {
    if (predicateColValues.isEmpty()) {
      throw new InternalException(this.getDomesticJdbcTable().getFullName() + " select has no predicates");
    }

    StringBuilder selectSqlBuilder = new StringBuilder();

    selectSqlBuilder.append("select ")
      .append(String.join(", ", this.getSelectColumnsSqlExpressions()))
      .append(" from ")
      .append(this.getDomesticJdbcTable().getFullName())
      .append(" ")
      .append(this.getDomesticJdbcTable().getName()); // alias

    if (this.foreignTables.size() > 1) {
      throw new InternalException("The Paginated Select supports for now a SQL with maximum 2 tables, not " + this.foreignTables + 1);
    }
    if (!this.foreignTables.isEmpty()) {
      JdbcTable joinedTable = this.foreignTables.iterator().next();
      selectSqlBuilder
        .append(" inner join ")
        .append(joinedTable.getFullName())
        .append(" ")
        .append(joinedTable.getName()) // name is alias for now
      ;
      /**
       * On columns
       */
      List<String> onSqlColumnPredicate = new ArrayList<>();
      Map<JdbcColumn, JdbcColumn> foreignKeyColumnMapping = this.getDomesticJdbcTable().getForeignKeyColumns(joinedTable);
      if (foreignKeyColumnMapping.isEmpty()) {
        throw new InternalException("The foreign key column mapping for the table (" + joinedTable + ") are not present in the table definition of (" + this.getDomesticJdbcTable() + ")");
      }
      for (Map.Entry<JdbcColumn, JdbcColumn> joinColumnMapping : foreignKeyColumnMapping.entrySet()) {
        onSqlColumnPredicate.add(this.getDomesticJdbcTable().getName() + "." + joinColumnMapping.getKey().getColumnName() + " = " + joinedTable.getName() + "." + joinColumnMapping.getValue().getColumnName());
      }
      selectSqlBuilder
        .append(" on ")
        .append(String.join(" and ", onSqlColumnPredicate));
    }

    /**
     * Where
     */
    selectSqlBuilder.append(" where ");

    /**
     * Predicates
     */
    List<Object> bindingValues = new ArrayList<>();
    List<String> predicateStatements = new ArrayList<>();
    for (JdbcSingleOperatorPredicate predicate : predicateColValues) {
      bindingValues.add(predicate.getValue());
      predicateStatements.add(predicate.toSql(bindingValues.size()));
    }
    selectSqlBuilder.append(String.join(" and ", predicateStatements));

    if (this.limit != null) {
      selectSqlBuilder.append(" LIMIT ").append(this.limit);
    }

    String selectSql = selectSqlBuilder.toString();


    return new JdbcPreparedStatement(selectSql, bindingValues);
  }

  /**
   * @param column - add a select column with its name as alias (default database)
   */
  public JdbcSelect addSelectColumn(JdbcColumn column) {
    return addSelectColumn(column, column.getColumnName());
  }

  public JdbcSelect addSelectColumn(JdbcColumn column, String alias) {
    Boolean foreignTableAdded = this.addEventuallyForeignTable(column);
    if (!foreignTableAdded) {
      return this;
    }
    if (!this.foreignSelectedColumns.containsKey(column)) {

      this.foreignSelectedColumns.put(column, alias);
    }
    return this;
  }

  /**
   * Add all columns from a table to the select
   * @param table - the table
   */
  public JdbcSelect addSelectAllColumnsFromTable(JdbcTable table) {
    this.foreignTables.add(table);
    for(JdbcColumn jdbcColumn: table.getColumns()){
      this.addSelectColumn(jdbcColumn);
    }
    return this;
  }

}
