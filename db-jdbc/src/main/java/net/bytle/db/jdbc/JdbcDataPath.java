package net.bytle.db.jdbc;

import net.bytle.db.model.TableDef;
import net.bytle.db.spi.DataPath;
import net.bytle.db.stream.SelectStream;

import java.sql.DatabaseMetaData;
import java.util.ArrayList;
import java.util.List;

/**
 * A jdbc data path knows only three parts
 * * catalog
 * * schema
 * * and name
 */
public class JdbcDataPath extends DataPath {


  public static final String CURRENT_WORKING_DIRECTORY = ".";
  private final JdbcDataSystem jdbcDataSystem;
  private final String name;
  private final String schema;
  private final String catalog;



  /**
   * TABLE", "VIEW", "SYSTEM TABLE", "GLOBAL TEMPORARY", "LOCAL TEMPORARY", "ALIAS", "SYNONYM".
   */
  private String type = TABLE_TYPE;
  public static final String TABLE_TYPE = "TABLE";
  public static final String VIEW_TYPE = "VIEW";
  public static final String QUERY_TYPE = "QUERY";

  private SqlSelectStream selectStream;


  public JdbcDataPath(JdbcDataSystem jdbcDataSystem, String catalog, String schema, String name) {
    this.jdbcDataSystem = jdbcDataSystem;
    this.catalog = catalog;
    this.schema = schema;
    this.name = name;
  }

  public JdbcDataPath(JdbcDataSystem jdbcDataSystem, String query) {
    this.jdbcDataSystem = jdbcDataSystem;
    this.setType(QUERY_TYPE);
    this.setQuery(query);
    this.name = null;
    this.schema = jdbcDataSystem.getCurrentSchema();
    this.catalog = jdbcDataSystem.getCurrentCatalog();
  }

  public static JdbcDataPath of(JdbcDataSystem jdbcDataSystem, String catalog, String schema, String name) {
    return new JdbcDataPath(jdbcDataSystem, catalog, schema, name);
  }

  public static JdbcDataPath ofQuery(JdbcDataSystem jdbcDataSystem, String query) {
    return new JdbcDataPath(jdbcDataSystem, query);
  }

  @Override
  public JdbcDataSystem getDataSystem() {
    return jdbcDataSystem;
  }

  @Override
  public TableDef getDataDef() {

    if (super.getDataDef().getColumnDefs().size() == 0) {
      // The data def of query is build at runtime
      if (type.equals(QUERY_TYPE)) {
        // The select stream build the data def
        selectStream = SqlSelectStream.of(this);
        dataDef = selectStream.getSelectDataDef();
        // sqlite for instance
        if (this.getDataSystem().getMaxWriterConnection()==1){
          selectStream.close();
          selectStream = null;
        }
      } else {
        Jdbcs.getTableDef(super.getDataDef());
      }
    }
    return super.getDataDef();

  }

  /**
   * {@link DatabaseMetaData#getMaxSchemaNameLength()}
   */
  public JdbcDataPath getSchema() {

    if (schema == null) {
      return null;
    } else {
      return JdbcDataPath.of(jdbcDataSystem, catalog, schema, null);
    }

  }

  /**
   * {@link DatabaseMetaData#getMaxTableNameLength()}
   */
  @Override
  public String getName() {
    if (name != null) {
      return name;
    }
    if (schema != null) {
      return schema;
    }
    if (catalog != null) {
      return catalog;
    }
    if (type.equals(QUERY_TYPE)) {
      return "query";
    }
    JdbcDataSystemLog.LOGGER_DB_JDBC.warning("All JDBC data path name are null (catalog, schema and name)");
    return null;
  }

  @Override
  public List<String> getPathParts() {
    List<String> pathSegments = new ArrayList<>();
    if (catalog != null) {
      pathSegments.add(catalog);
    }
    if (schema != null) {
      pathSegments.add(schema);
    }
    if (name != null) {
      pathSegments.add(name);
    }
    return pathSegments;
  }

  @Override
  public String getPath() {
    return String.join(".", getPathParts());
  }


  public String getCatalog() {
    return catalog;
  }


  public boolean isDataUnit() {
    return name != null;
  }

  public JdbcDataPath setType(String type) {
    this.type = type;
    return this;
  }

  public String getType() {
    return this.type;
  }

  public SelectStream getSelectStream() {

    // When it's a query, the select stream has already been created to
    // get the data def
    if (selectStream==null){
      selectStream = SqlSelectStream.of(this);
    }
    return selectStream;

  }


}
