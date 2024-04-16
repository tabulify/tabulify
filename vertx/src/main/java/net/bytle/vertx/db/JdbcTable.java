package net.bytle.vertx.db;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

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


  public static class JdbcTableBuilder {
    private final JdbcSchema jdbcSchema;
    private final String name;

    private final HashSet<JdbcTableColumn> jdbcPrimaryOrUniqueKeyColumns = new HashSet<>();

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
