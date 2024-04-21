package net.bytle.vertx.db;

import java.util.HashMap;
import java.util.Map;

/**
 * A Sql Engine is:
 * * The declared schema for a database
 * * utilities to help creating sql
 * <p>
 * The metadata is based on:
 * * the registration of table object
 * * and enum columns
 */
public class JdbcSqlStatementEngine {



  private final Map<JdbcColumn, JdbcTable> columnToTableMap = new HashMap<>();
  private final Map<JdbcColumn,JdbcColumn> foreignKeyColumnsMapping = new HashMap<>();

  public Map<JdbcColumn, JdbcColumn> getForeignKeyColumns(JdbcTable domesticTable, JdbcTable foreignTable) {
    Map<JdbcColumn, JdbcColumn> foreignKeys = new HashMap<>();
    for(Map.Entry<JdbcColumn,JdbcColumn> foreignKeyColumnsMapping:this.foreignKeyColumnsMapping.entrySet()){
      JdbcTable domesticKeyColumnTable = this.getTableOfColumn(foreignKeyColumnsMapping.getKey());
      if(!domesticKeyColumnTable.equals(domesticTable)){
        continue;
      }
      JdbcTable foreignValueColumnTable = this.getTableOfColumn(foreignKeyColumnsMapping.getValue());
      if(!foreignValueColumnTable.equals(foreignTable)){
        continue;
      }
      foreignKeys.put(foreignKeyColumnsMapping.getKey(),foreignKeyColumnsMapping.getValue());
    }
    return foreignKeys;
  }

  /**
   * When a table is build, it registers itself
   */
  public void registerTable(JdbcTable jdbcTable) {
    for(JdbcColumn jdbcColumn : jdbcTable.getColumns()){
      this.columnToTableMap.put(jdbcColumn, jdbcTable);
    }
  }

  /**
   * When a table is build with foreign keys, it registers its foreign keys
   */
  public void registerForeignKey(Map<JdbcColumn, JdbcColumn> columnsMapping) {
    this.foreignKeyColumnsMapping.putAll(columnsMapping);
  }

  public JdbcTable getTableOfColumn(JdbcColumn jdbcColumn){
    return this.columnToTableMap.get(jdbcColumn);
  }

  /**
   *
   * @param column - the column
   * @return the full column name (with table)
   */
  public String toFullColumnName(JdbcColumn column) {
    return this.getTableOfColumn(column).getName()+"."+column.getColumnName();
  }

}
