package net.bytle.vertx.db;

import java.util.HashSet;
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

  public Set<JdbcTableColumn> getPrimaryKeyColumns() {
    return this.jdbcTableBuilder.jdbcKeyColumns;
  }


  public static class JdbcTableBuilder {
    private final JdbcSchema jdbcSchema;
    private final String name;

    private final HashSet<JdbcTableColumn> jdbcKeyColumns = new HashSet<>();

    public JdbcTableBuilder(JdbcSchema jdbcSchema, String name) {
      this.jdbcSchema = jdbcSchema;
      this.name = name;
    }

    public JdbcTableBuilder addPrimaryKeyColumn(JdbcTableColumn jdbcTableColumn){
      this.jdbcKeyColumns.add(jdbcTableColumn);
      return this;
    }

    public JdbcTable build() {
      return new JdbcTable(this);
    }

  }
}
