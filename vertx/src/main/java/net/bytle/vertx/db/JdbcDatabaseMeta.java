package net.bytle.vertx.db;

import net.bytle.exception.InternalException;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * The declared schema for a database
 */
public class JdbcDatabaseMeta {


  private Set<JdbcTable> tables = new HashSet<>();

  Map<JdbcTableColumn, JdbcTableColumn> foreignKeyColumnsMapping = new HashMap<>();

  public Map<JdbcTableColumn, JdbcTableColumn> getForeignKeyColumns(JdbcTable jdbcTable, JdbcTable joinedTable) {
    return null;
  }

  public void registerTable(JdbcTable jdbcTable) {
    this.tables.add(jdbcTable);
  }

  public void buildGraph() {
    throw new InternalException("To finish");
  }
}
