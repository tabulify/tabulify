package net.bytle.vertx.db;

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

  public static JdbcTableBuilder build(JdbcSchema jdbcSchema, String name) {
    return new JdbcTableBuilder(jdbcSchema, name);
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
    return this.jdbcTableBuilder.foreignKeys.get(joinedTable);
  }


  public static class JdbcTableBuilder {
    private final JdbcSchema jdbcSchema;
    private final String name;

    private final HashSet<JdbcTableColumn> jdbcPrimaryOrUniqueKeyColumns = new HashSet<>();
    private final Map<JdbcTable, Map<JdbcTableColumn, JdbcTableColumn>> foreignKeys = new HashMap<>();

    public JdbcTableBuilder(JdbcSchema jdbcSchema, String name) {
      this.jdbcSchema = jdbcSchema;
      this.name = name;
    }

    public JdbcTableBuilder addPrimaryKeyColumn(JdbcTableColumn jdbcTableColumn){
      this.jdbcPrimaryOrUniqueKeyColumns.add(jdbcTableColumn);
      return this;
    }

    public JdbcTable build() {
      return new JdbcTable(this);
    }

    public JdbcTableBuilder addUniqueKeyColumn(JdbcTableColumn tableColumn) {
      this.jdbcPrimaryOrUniqueKeyColumns.add(tableColumn);
      return this;
    }

    public JdbcTableBuilder addForeignKeyColumns(JdbcTable userTable, Map<JdbcTableColumn, JdbcTableColumn> columnsMapping) {
      this.foreignKeys.put(userTable, columnsMapping);
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
