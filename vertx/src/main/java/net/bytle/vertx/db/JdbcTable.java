package net.bytle.vertx.db;

import net.bytle.exception.InternalException;

import java.util.*;

/**
 * A simple SQL table abstraction
 * for secure update
 */
public class JdbcTable {


  private final JdbcTableBuilder jdbcTableBuilder;

  private JdbcTable(JdbcTableBuilder jdbcTableBuilder) {
    this.jdbcTableBuilder = jdbcTableBuilder;

  }

  public static JdbcTableBuilder build(JdbcSchema jdbcSchema, String name, JdbcTableColumn[] tableColumns) {
    return new JdbcTableBuilder(jdbcSchema, name, tableColumns);
  }


  /**
   *
   * @return the name and the schema
   */
  public String getFullName() {
    return this.jdbcTableBuilder.jdbcSchema.getSchemaName() + "." + jdbcTableBuilder.name;
  }


  public JdbcSchema getSchema(){
    return this.jdbcTableBuilder.jdbcSchema;
  }

  public Set<JdbcTableColumn> getPrimaryOrUniqueKeyColumns() {
    return this.jdbcTableBuilder.jdbcPrimaryOrUniqueKeyColumns;
  }

  public String getName() {
    return this.jdbcTableBuilder.name;
  }

  public Map<JdbcTableColumn, JdbcTableColumn> getForeignKeyColumns(JdbcTable joinedTable) {
    return this.jdbcTableBuilder.jdbcSchema.getJdbcClient().getDatabaseMeta().getForeignKeyColumns(this,joinedTable);
  }


  public static class JdbcTableBuilder {
    private final JdbcSchema jdbcSchema;
    private final String name;

    private final HashSet<JdbcTableColumn> jdbcPrimaryOrUniqueKeyColumns = new HashSet<>();
    private final  Map<JdbcTableColumn, JdbcTableColumn> foreignKeys = new HashMap<>();
    private final Set<JdbcTableColumn> columns;

    public <T extends Enum<T> & JdbcTableColumn> JdbcTableBuilder(JdbcSchema jdbcSchema, String name, JdbcTableColumn[] columns) {
      this.jdbcSchema = jdbcSchema;
      this.name = name;
      this.columns = new HashSet<>(List.of(columns));
    }

    public JdbcTableBuilder addPrimaryKeyColumn(JdbcTableColumn jdbcTableColumn){
      this.checkColumn(jdbcTableColumn);
      this.jdbcPrimaryOrUniqueKeyColumns.add(jdbcTableColumn);
      return this;
    }

    private void checkColumn(JdbcTableColumn jdbcTableColumn) {
      if(!columns.contains(jdbcTableColumn)){
        throw new InternalException("The column ("+jdbcTableColumn+") is not a declared column of the table ("+this+")");
      }
    }

    public JdbcTable build() {

      JdbcTable jdbcTable = new JdbcTable(this);
      this.jdbcSchema.getJdbcClient().getDatabaseMeta().registerTable(jdbcTable);
      return jdbcTable;
    }

    public JdbcTableBuilder addUniqueKeyColumn(JdbcTableColumn tableColumn) {
      this.checkColumn(tableColumn);
      this.jdbcPrimaryOrUniqueKeyColumns.add(tableColumn);
      return this;
    }

    public JdbcTableBuilder addForeignKeyColumns(Map<JdbcTableColumn, JdbcTableColumn> columnsMapping) {
      this.foreignKeys.putAll(columnsMapping);
      return this;
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    JdbcTable jdbcTable = (JdbcTable) o;
    return Objects.equals(this.getFullName(), jdbcTable.getFullName());
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.getFullName());
  }

}
