package com.tabulify.postgres;

import com.tabulify.fs.sql.SqlPlusLexer;
import com.tabulify.jdbc.SqlConnection;
import com.tabulify.jdbc.SqlDataPath;
import com.tabulify.spi.DataPath;
import net.bytle.exception.NoCatalogException;
import net.bytle.exception.NoSchemaException;
import net.bytle.type.MediaType;
import net.bytle.type.Strings;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

public class PostgresDataPath extends SqlDataPath {


  public PostgresDataPath(SqlConnection jdbcDataStore, String path, MediaType sqlType) {
    super(jdbcDataStore, path, sqlType);
  }

  /**
   * https://www.postgresql.org/docs/9.5/ddl-depend.html
   * The dependent table is pg_dependent
   * https://www.postgresql.org/docs/9.5/catalog-pg-depend.html
   * <p>
   * <p>
   * A namespace in Postgres is a schema
   * <p>
   * Based on:
   * https://stackoverflow.com/questions/4462908/find-dependent-objects-for-a-table-or-view
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
    String queryDependencies;
    try (SqlPlusLexer fromString = SqlPlusLexer.createFromString(script)) {
      queryDependencies = fromString.getSqlStatements().get(0);
    }
    try (PreparedStatement statement = this.getConnection().getCurrentConnection().prepareStatement(queryDependencies)) {
      statement.setString(1, this.getName());
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
        if (!(schemaName + this.getName()).equals(schema + name)) {
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
