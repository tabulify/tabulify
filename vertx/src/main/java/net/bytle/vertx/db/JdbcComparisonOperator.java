package net.bytle.vertx.db;

public enum JdbcComparisonOperator {

  EQUALITY("="),
  LESS_THAN("<"),
  NOT_EQUAL("!="),
  LIKE("like");

  private final String sqlSyntax;

  JdbcComparisonOperator(String sqlSyntax) {
    this.sqlSyntax = sqlSyntax;
  }

  public String toSql() {
    return this.sqlSyntax;
  }
}
