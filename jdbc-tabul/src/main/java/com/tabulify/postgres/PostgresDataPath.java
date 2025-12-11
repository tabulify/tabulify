package com.tabulify.postgres;

import com.tabulify.fs.sql.SqlLexer;
import com.tabulify.fs.sql.SqlStatement;
import com.tabulify.jdbc.SqlConnection;
import com.tabulify.jdbc.SqlDataPath;
import com.tabulify.spi.DataPath;
import com.tabulify.exception.NoCatalogException;
import com.tabulify.exception.NoSchemaException;
import com.tabulify.type.MediaType;
import com.tabulify.type.Strings;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

public class PostgresDataPath extends SqlDataPath {


  public PostgresDataPath(SqlConnection jdbcDataStore, String path, MediaType sqlType) {
    super(jdbcDataStore, path, null, sqlType);
  }

  /**
   * <a href="https://www.postgresql.org/docs/9.5/ddl-depend.html">...</a>
   * The dependent table is pg_dependent
   * <a href="https://www.postgresql.org/docs/9.5/catalog-pg-depend.html">...</a>
   * <p>
   * <p>
   * A namespace in Postgres is a schema
   * <p>
   * Based on:
   * <a href="https://stackoverflow.com/questions/4462908/find-dependent-objects-for-a-table-or-view">...</a>
   */
  @Override
  public Set<DataPath> getDependencies() {
    /**
     * View dependency
     * In postgres, a table cannot be dropped
     * if a view depends on it
     * This dependency is in the class  pg_rewrite
     * The below query return them
     */
    Set<DataPath> dependencies = new HashSet<>();
    String script = Strings.createFromResource(PostgresDataPath.class, "/sql/postgresQueryDependencies.sql").toString();
    String queryDependencies = SqlLexer.parseFromString(script)
      .stream()
      .filter(s -> s.getKind().isSelect())
      .map(SqlStatement::getStatement)
      .findFirst()
      .orElseThrow();
    try (PreparedStatement statement = this.getConnection().getCurrentJdbcConnection().prepareStatement(queryDependencies)) {
      String tableName = this.getLogicalName();
      statement.setString(1, tableName);
      String schemaName;
      try {
        schemaName = this.getSchema().getName();
      } catch (NoSchemaException e) {
        schemaName = null;
      }
      statement.setString(2, schemaName);
      ResultSet resultSet = statement.executeQuery();
      while (resultSet.next()) {
        String name = resultSet.getString(1);
        String schema = resultSet.getString(2);

        /**
         * As a query depends on itself for whatever reason,
         * and that we don't want any loop, we block it with this condition
         */
        if (!(schemaName + tableName).equals(schema + name)) {
          String catalogName;
          try {
            catalogName = this.getCatalogDataPath().getName();
          } catch (NoCatalogException e) {
            catalogName = null;
          }
          dependencies.add(this.getConnection().createSqlDataPath(catalogName, schema, name));
        }
      }
    } catch (SQLException e) {
      throw new RuntimeException("Bad query: " + Strings.createFromString(queryDependencies).onOneLine().toString(), e);
    }
    /**
     * Add the foreign key dependencies
     */
    dependencies.addAll(super.getDependencies());
    /**
     * Return the set
     */
    return dependencies;
  }

  /**
   * See column `relpages` from
   * <a href="https://www.postgresql.org/docs/9.5/catalog-pg-class.html">...</a>
   */
  @Override
  public Long getSize() {
    return super.getSize();
  }

  /**
   * See column `reltuples` from
   * <a href="https://www.postgresql.org/docs/9.5/catalog-pg-class.html">...</a>
   */
  @Override
  public Long getCount() {
    return super.getCount();
  }

}
