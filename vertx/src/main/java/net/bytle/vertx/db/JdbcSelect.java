package net.bytle.vertx.db;

import io.vertx.core.Future;
import io.vertx.sqlclient.SqlConnection;
import io.vertx.sqlclient.Tuple;
import net.bytle.exception.InternalException;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class JdbcSelect extends JdbcQuery {


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
  private final Set<JdbcTable> innerJoinTables = new HashSet<>();

  /**
   * Extra Selected columns (that does not belong to the main table)
   * By table, the column and their alias
   */
  private final Map<JdbcTable, Map<JdbcTableColumn, String>> extraSelectedColumns = new HashMap<>();

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

    JdbcPreparedStatement preparedStatement = this.toPreparedStatement();
    return sqlConnection
      .preparedQuery(preparedStatement.getPreparedSql())
      .execute(Tuple.from(preparedStatement.getBindingValues()))
      .recover(e -> Future.failedFuture(new InternalException(this.getJdbcTable().getFullName() + " table select Error. Sql Error " + e.getMessage() + "\nSQl: " + preparedStatement, e)))
      .compose(rowSet -> Future.succeededFuture(new JdbcRowSet(rowSet)));
  }

  public JdbcSelect addLimit(Long limit) {
    this.limit = limit;
    return this;
  }

  @NotNull
  private List<String> getSelectColumnsSqlExpressions() {

    List<String> selectColumnsSqlExpressions = new ArrayList<>(this.selectExpressions);
    selectColumnsSqlExpressions.add(this.getJdbcTable().getName() + ".* ");
    for (Map.Entry<JdbcTable, Map<JdbcTableColumn, String>> selectTable : this.extraSelectedColumns.entrySet()) {
      for (Map.Entry<JdbcTableColumn, String> selectColumn : selectTable.getValue().entrySet()) {
        selectColumnsSqlExpressions.add(selectTable.getKey().getName() + "." + selectColumn.getKey().getColumnName() + " as " + selectColumn.getValue());
      }
    }
    return selectColumnsSqlExpressions;
  }

  public void addSelectExpression(String selectExpression) {
    this.selectExpressions.add(selectExpression);
  }

  public JdbcSelect addEqualityPredicate(JdbcTable table, JdbcTableColumn tableColumn, Object value) {
    this.addEventuallyInnerJoinTable(table);
    this.predicateColValues.add(
      JdbcSingleOperatorPredicate.builder()
        .setColumn(table, tableColumn, value)
        .build()
    );
    return this;
  }

  private void addEventuallyInnerJoinTable(JdbcTable potentialInnerTable) {
    if(!potentialInnerTable.equals(this.getJdbcTable())) {
      this.innerJoinTables.add(potentialInnerTable);
    }
  }

  public JdbcPreparedStatement toPreparedStatement() {
    if (predicateColValues.isEmpty()) {
      throw new InternalException(this.getJdbcTable().getFullName() + " select has no predicates");
    }

    StringBuilder selectSqlBuilder = new StringBuilder();

    selectSqlBuilder.append("select ")
      .append(String.join(", ",this.getSelectColumnsSqlExpressions()))
      .append(" from ")
      .append(this.getJdbcTable().getFullName())
      .append(" ")
      .append(this.getJdbcTable().getName()) // alias
      .append(" where ");

    if (this.innerJoinTables.size() > 1) {
      throw new InternalException("The Paginated Select supports for now a SQL with maximum 2 tables, not " + this.innerJoinTables + 1);
    }
    if (!this.innerJoinTables.isEmpty()) {
      JdbcTable joinedTable = this.innerJoinTables.iterator().next();
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
      Map<JdbcTableColumn, JdbcTableColumn> foreignKeyColumnMapping = this.getJdbcTable().getForeignKeyColumns(joinedTable);
      if (foreignKeyColumnMapping.isEmpty()) {
        throw new InternalException("The foreign key column mapping for the table (" + joinedTable + ") are not present in the table definition of (" + this.getJdbcTable() + ")");
      }
      for (Map.Entry<JdbcTableColumn, JdbcTableColumn> joinColumnMapping : foreignKeyColumnMapping.entrySet()) {
        onSqlColumnPredicate.add(this.getJdbcTable().getName() + "." + joinColumnMapping.getKey().getColumnName() + " = " + joinedTable.getName() + "." + joinColumnMapping.getValue().getColumnName());
      }
      selectSqlBuilder
        .append(" on ")
        .append(String.join(" and ", onSqlColumnPredicate));
    }

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

    String insertSqlString = selectSqlBuilder.toString();


    return  new JdbcPreparedStatement(insertSqlString,bindingValues);
  }

  public JdbcSelect addExtraSelectColumn(JdbcTable jdbcTable, JdbcTableColumn column) {
    this.addEventuallyInnerJoinTable(jdbcTable);
    Map<JdbcTableColumn, String> tableColumns = this.extraSelectedColumns.computeIfAbsent(jdbcTable, key -> new HashMap<>());
    tableColumns.put(column, column.getColumnName());
    return this;
  }
}
