package com.tabulify.jdbc;

import com.tabulify.fs.FsDataPath;
import com.tabulify.fs.sql.*;
import com.tabulify.model.ColumnDef;
import com.tabulify.model.RelationDef;
import com.tabulify.spi.DataPath;
import com.tabulify.spi.StrictException;
import com.tabulify.spi.Tabulars;
import com.tabulify.stream.SelectStream;
import net.bytle.exception.InternalException;
import net.bytle.type.KeyNormalizer;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A wrapper utility around a SQL File
 */
public class SqlScript {


  private final List<SqlStatement> statements;
  private final SqlScriptBuilder builder;

  private SqlScript(SqlScriptBuilder sqlScriptBuilder) {

    this.builder = sqlScriptBuilder;
    if (sqlScriptBuilder.executableDataPath == null && sqlScriptBuilder.sqlString == null) {
      throw new InternalException("A executable resource or an execution query is required. None was found.");
    }

    /**
     * Statements
     */
    if (sqlScriptBuilder.sqlString != null) {
      this.statements = SqlLexer.parseFromString(sqlScriptBuilder.sqlString);
      if (this.statements.isEmpty()) {
        if (sqlScriptBuilder.executableDataPath.getConnection().getTabular().isStrictExecution()) {
          throw new IllegalArgumentException("No statements found in the sql string");
        }
      }
    } else {
      this.statements = getSqlTokens(sqlScriptBuilder.executableDataPath);
      if (this.statements.isEmpty()) {
        if (sqlScriptBuilder.executableDataPath.getConnection().getTabular().isStrictExecution()) {
          throw new IllegalArgumentException("No statements found in " + sqlScriptBuilder.executableDataPath);
        }
      }
    }


  }


  /**
   * Return the sql tokens from file or database
   */
  private static List<SqlStatement> getSqlTokens(DataPath executionDataPath) {

    if (!Tabulars.exists(executionDataPath)) {
      if (executionDataPath.getConnection().getTabular().isStrictExecution()) {
        throw new StrictException("The sql execution data resource (" + executionDataPath + ") does not exist, we can't get SQL statements.");
      }
      /**
       * No statement is possible when we just want to test a path
       */
      return List.of();
    }
    if (executionDataPath instanceof FsSqlDataPath) {
      return SqlLexer.parseFromPath(((FsDataPath) executionDataPath).getAbsoluteNioPath());
    }
    /**
     * Not a sql file, try to get the sql from the sql column value
     */
    RelationDef relationDef = executionDataPath.getOrCreateRelationDef();
    List<ColumnDef> columnDefs = relationDef.getColumnDefs();
    ColumnDef columnDef;
    if (columnDefs.size() == 1) {
      columnDef = columnDefs.get(0);
    } else {
      KeyNormalizer sqlKeyColumn = FsSqlParserColumn.SQL.toKeyNormalizer();
      columnDef = relationDef.getColumnDef(sqlKeyColumn);
      if (columnDef == null) {
        throw new IllegalArgumentException("A non-file executable sql resource should have the " + sqlKeyColumn.toSqlCase() + " column. The resource (" + executionDataPath + ") has only the following columns: " + columnDefs.stream().map(ColumnDef::getColumnName).collect(Collectors.joining(", ")));
      }
    }
    List<SqlStatement> sqlStatements = new ArrayList<>();
    try (SelectStream selectStream = executionDataPath.getSelectStreamSafe()) {

      while (selectStream.next()) {
        String sql = selectStream.getObject(columnDef, String.class);
        if (sql == null || sql.isEmpty()) {
          throw new IllegalArgumentException("The column  " + columnDef + " has a value that is null or empty in the record " + selectStream.getRecordId() + " of the resource (" + executionDataPath + ")");
        }
        sqlStatements.addAll(SqlLexer.parseFromString(sql));
      }
    }
    return sqlStatements;
  }

  public static SqlScriptBuilder builder() {


    return new SqlScriptBuilder();
  }

  /**
   * Utility function
   *
   * @return the select statement
   * @throws IllegalArgumentException if the sql script is not a single select statement
   */
  public String getSelect() {
    return this.statements.stream().filter(s -> s.getKind().isSelect())
      .map(SqlStatement::getStatement)
      .findFirst()
      .orElseThrow(() -> new IllegalArgumentException("The executable sql script does not have any SELECT statement"))
      // trim for normalization as we don't have yet an AST
      .trim();
  }

  /**
   * @return if this script is a single select statement usable as sub-select in a view or select statement
   */
  public boolean isSingleSelectStatement() {
    long selectStatementCount = this.statements.stream()
      .filter(s ->
        s.getKind()
          .isSelect())
      .count();
    return selectStatementCount == 1;
  }


  @Override
  public String toString() {

    return builder.executableDataPath.toDataUri().toString();

  }


  /**
   * @return the statements without the script comment
   */
  public List<SqlStatement> getExecutableStatements() {
    return this.statements
      .stream()
      .filter(s -> !s.getKind().equals(SqlStatementKind.SCRIPT_COMMENT))
      .collect(Collectors.toList());
  }


  public DataPath getExecutableDataPath() {
    return builder.executableDataPath;
  }


  public static class SqlScriptBuilder {

    private DataPath executableDataPath;

    private String sqlString;


    public SqlScriptBuilder setExecutableDataPath(DataPath executableDataPath) {

      // check
      // Note that it could be a query,
      // ie: ((query.sql@cd)@sqlite)@sqlite
      this.executableDataPath = executableDataPath;
      return this;

    }

    public SqlScript build() {
      return new SqlScript(this);
    }


    /**
     * @param executableDataPath - the executable data path is mandatory because this is the identifier
     * @param sql                - the sql
     */
    public SqlScriptBuilder setSqlString(DataPath executableDataPath, String sql) {
      if (sql.trim().isEmpty()) {
        throw new IllegalArgumentException("The sql string cannot be empty");
      }
      this.sqlString = sql;
      this.executableDataPath = executableDataPath;
      return this;
    }

    /**
     * When the execution query is a `select` of a {@link SqlDataPath object} (ie table, view)
     */
    public SqlScriptBuilder setSqlDataPath(SqlDataPath sqlDataPath) {
      this.sqlString = "select * from " + sqlDataPath.getConnection().getDataSystem().createFromClause(sqlDataPath);
      this.executableDataPath = sqlDataPath;
      return this;
    }

  }
}
