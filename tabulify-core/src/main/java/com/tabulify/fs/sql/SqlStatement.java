/*
 * Copyright (c) 2014. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package com.tabulify.fs.sql;


/**
 * Internal statement representation.
 * (the token of the {@link SqlLexer}
 * <p/>
 * It is used as contract between the lexer and the parser.
 */
public final class SqlStatement {

  /**
   * length of the initial token (content-)buffer
   */

  private final SqlStatementBuilder sqlStatementBuilder;


  private SqlStatement(SqlStatementBuilder sqlStatementBuilder) {
    this.sqlStatementBuilder = sqlStatementBuilder;
  }

  public static SqlStatementBuilder builder() {
    return new SqlStatementBuilder();
  }


  /**
   * Eases IDE debugging.
   *
   * @return a string helpful for debugging.
   */
  @Override
  public String toString() {

    if (sqlStatementBuilder.kind != null) {
      return sqlStatementBuilder.kind.name() + " [" + sqlStatementBuilder.statement + "]";
    } else {
      return "Token Unknown";
    }

  }

  public String getStatement() {
    return sqlStatementBuilder.statement.toString();
  }

  public int getLine() {
    return sqlStatementBuilder.line;
  }

  public SqlStatementKind getKind() {
    return sqlStatementBuilder.kind;
  }

  public static class SqlStatementBuilder {
    private static final int INITIAL_TOKEN_LENGTH = 50;
    private SqlStatementKind kind;
    /**
     * The builder that fills while scanning with
     * the statement words
     */
    private final StringBuilder statement = new StringBuilder(INITIAL_TOKEN_LENGTH);
    private int line;

    public SqlStatement build() {
      assert this.kind != null : "The statement kind should not be null";
      assert this.statement.length() != 0 : "The statement length should not be null";
      return new SqlStatement(this);
    }

    public SqlStatementKind getKind() {
      return this.kind;
    }

    public SqlStatementBuilder setKind(SqlStatementKind statementKind) {
      this.kind = statementKind;
      return this;
    }

    public SqlStatementBuilder append(String string) {
      statement.append(string);
      return this;
    }

    public SqlStatementBuilder setLineNumber(int lineCounter) {
      this.line = lineCounter;
      return this;
    }
  }
}
