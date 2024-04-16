package net.bytle.vertx.db;

public enum JdbcSort {

  ASC("ASC"),
  DESC("DESC");

  private final String sqlSyntax;

  JdbcSort(String sqlSyntax) {
    this.sqlSyntax = sqlSyntax;
  }

  public String toSql() {
    return this.sqlSyntax;
  }

}
