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

  public static JdbcTableBuilder build(JdbcSchema jdbcSchema, String name, JdbcColumn[] tableColumns) {
    return new JdbcTableBuilder(jdbcSchema, name, tableColumns);
  }


  /**
   *
   * @return the name and the schema
   */
  public String getFullName() {
    return this.jdbcTableBuilder.jdbcSchema.getSchemaName() + "." + jdbcTableBuilder.name;
  }


  public JdbcSchema getSchema() {
    return this.jdbcTableBuilder.jdbcSchema;
  }

  public Set<JdbcColumn> getPrimaryKeyColumns() {
    return this.jdbcTableBuilder.jdbcPrimaryKeyColumns;
  }

  public String getName() {
    return this.jdbcTableBuilder.name;
  }

  public Map<JdbcColumn, JdbcColumn> getForeignKeyColumns(JdbcTable joinedTable) {
    return this.jdbcTableBuilder.jdbcSchema.getJdbcClient().getSqlStatementEngine().getForeignKeyColumns(this, joinedTable);
  }

  public Set<JdbcColumn> getColumns() {
    return this.jdbcTableBuilder.columns;
  }

  public boolean hasColumn(JdbcColumn jdbcColumn) {
    return this.jdbcTableBuilder.columns.contains(jdbcColumn);
  }


  public static class JdbcTableBuilder {
    private final JdbcSchema jdbcSchema;
    private final String name;

    private final Set<JdbcColumn> jdbcPrimaryKeyColumns = new HashSet<>();
    private final List<Set<JdbcColumn>> jdbcUniqueKeyColumns = new ArrayList<>();

    private final Set<JdbcColumn> columns;

    public JdbcTableBuilder(JdbcSchema jdbcSchema, String name, JdbcColumn[] columns) {
      this.jdbcSchema = jdbcSchema;
      this.name = name;
      this.columns = new HashSet<>(List.of(columns));
    }

    public JdbcTableBuilder addPrimaryKeyColumn(JdbcColumn jdbcColumn) {
      this.checkColumn(jdbcColumn);
      this.jdbcPrimaryKeyColumns.add(jdbcColumn);
      return this;
    }

    private void checkColumn(JdbcColumn jdbcColumn) {
      if (!columns.contains(jdbcColumn)) {
        throw new InternalException("The column (" + jdbcColumn + ") is not a declared column of the table (" + this + ")");
      }
    }

    public JdbcTable build() {

      JdbcTable jdbcTable = new JdbcTable(this);
      this.jdbcSchema.getJdbcClient().getSqlStatementEngine().registerTable(jdbcTable);
      return jdbcTable;
    }

    /**
     * Add a unique key of 1 domestic column
     */
    public JdbcTableBuilder addUniqueKeyColumn(JdbcColumn tableColumn) {
      this.checkColumn(tableColumn);
      this.jdbcUniqueKeyColumns.add(Set.of(tableColumn));
      return this;
    }

    /**
     * Add a foreign key with column mappings
     */
    public JdbcTableBuilder addForeignKeyColumns(Map<JdbcColumn, JdbcColumn> columnsMapping) {
      // first column should be the first party/domestic table
      for (JdbcColumn jdbcColumn : columnsMapping.keySet()) {
        this.checkColumn(jdbcColumn);
      }
      this.jdbcSchema.getJdbcClient().getSqlStatementEngine().registerForeignKey(columnsMapping);
      return this;
    }

    /**
     * Add a simple column foreign key from one domestic column (pk, unique) to another foreign column (pk, unique)
     * Don't use this function, if the foreign key has multiple columns, use {@link #addForeignKeyColumns(Map)}
     * instead
     */
    public JdbcTableBuilder addForeignKeyColumn(JdbcColumn domesticColumn, JdbcColumn foreignColumn) {
      Map<JdbcColumn, JdbcColumn> foreign = new HashMap<>();
      this.checkColumn(domesticColumn);
      foreign.put(domesticColumn, foreignColumn);
      addForeignKeyColumns(foreign);
      return this;
    }

    /**
     * Add a unique key of 2 domestic columns
     */
    public JdbcTableBuilder addUniqueKeyColumns(JdbcColumn domesticColumn, JdbcColumn domesticColumn1) {
      this.checkColumn(domesticColumn);
      this.checkColumn(domesticColumn1);
      this.jdbcUniqueKeyColumns.add(Set.of(domesticColumn, domesticColumn1));
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
