package com.tabulify.jdbc;

import com.tabulify.spi.DropTruncateAttribute;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A drop statement is pretty standardized
 * This is a helper to create them
 */
public class SqlDropStatement {

  private final DropStatementBuilder builder;

  public SqlDropStatement(DropStatementBuilder dropStatementBuilder) {
    this.builder = dropStatementBuilder;
  }

  public static DropStatementBuilder builder() {
    return new DropStatementBuilder();
  }

  public List<String> getStatements(List<SqlDataPath> sqlDataPaths, Set<DropTruncateAttribute> dropAttributes) {

    /**
     * Multiple object supported
     * ie drop table name1, name2
     */
    if (this.builder.multipleMediaSupported) {
      return List.of(this.getStatementsLocal(sqlDataPaths, dropAttributes));
    }

    /**
     * Multiple object not supported
     * ie drop table name1
     * ie drop table name2
     */
    List<String> statements = new ArrayList<>();
    for (SqlDataPath sqlDataPath : sqlDataPaths) {
      statements.add(this.getStatementsLocal(List.of(sqlDataPath), dropAttributes));
    }
    return statements;

  }

  private String getStatementsLocal(List<SqlDataPath> sqlDataPaths, Set<DropTruncateAttribute> dropAttributes) {

    StringBuilder dropTableStatement = new StringBuilder();
    dropTableStatement.append("drop ");

    SqlMediaType sqlMediaType = this.builder.sqlMediaType;
    switch (sqlMediaType) {
      case TABLE:
        dropTableStatement.append("table ");
        break;
      case VIEW:
        dropTableStatement.append("view ");
        break;
      case SCHEMA:
        // Example doc: https://www.postgresql.org/docs/current/sql-dropschema.html
        dropTableStatement.append("schema ");
        break;
      default:
        throw new RuntimeException("The drop of the SQL object type (" + sqlMediaType + ") is not implemented");
    }

    if (dropAttributes.contains(DropTruncateAttribute.IF_EXISTS)) {
      if (this.builder.ifExistsSupported) {
        dropTableStatement.append(" if exists ");
      }
    }


    String names = sqlDataPaths.stream().map(
      d -> {
        if (this.builder.maximumNameParts == null) {
          return d.toSqlStringPath();
        }
        return d.toSqlStringPath(this.builder.maximumNameParts);
      }
    ).collect(Collectors.joining(", "));
    dropTableStatement.append(names);
    if (dropAttributes.contains(DropTruncateAttribute.CASCADE)) {
      if (this.builder.cascadeSupported) {
        dropTableStatement.append(" ").append(this.builder.cascadeWord);
      }
    }
    return dropTableStatement.toString();
  }


  public static class DropStatementBuilder {
    private SqlMediaType sqlMediaType;
    private boolean cascadeSupported = true;
    private boolean multipleMediaSupported = true;
    private boolean ifExistsSupported = true;
    private Integer maximumNameParts = null;
    private String cascadeWord = "cascade";

    /**
     * Object type to drop
     */
    public DropStatementBuilder setType(SqlMediaType sqlMediaType) {
      this.sqlMediaType = sqlMediaType;
      return this;
    }

    /**
     * Do we support the CASCADE
     */
    public DropStatementBuilder setIsCascadeSupported(boolean b) {
      cascadeSupported = b;
      return this;
    }

    /**
     * Do we support
     * drop table name1, name2
     */
    public DropStatementBuilder setMultipleSqlObjectSupported(boolean b) {
      multipleMediaSupported = b;
      return this;
    }

    /**
     * Do we support the IF EXISTS
     */
    public DropStatementBuilder setIfExistsSupported(boolean b) {
      ifExistsSupported = b;
      return this;
    }

    public SqlDropStatement build() {
      return new SqlDropStatement(this);
    }

    /**
     * The maximum number of part in the sql name
     * Why?
     * Sql Server 'DROP VIEW' does not allow specifying the database name as a prefix to the object name.
     */
    public DropStatementBuilder setMaximumNamePart(int i) {
      maximumNameParts = i;
      return this;
    }

    public DropStatementBuilder setCascadeWord(String cascadeConstraints) {
      cascadeWord = cascadeConstraints;
      return this;
    }
  }
}
