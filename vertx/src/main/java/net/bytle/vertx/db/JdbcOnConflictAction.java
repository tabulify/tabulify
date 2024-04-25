package net.bytle.vertx.db;

public enum JdbcOnConflictAction {

  DO_NOTHING("do nothing");

  private final String sql;

  JdbcOnConflictAction(String sql) {
    this.sql = sql;
  }

  public String getSql() {
    return sql;
  }
}
