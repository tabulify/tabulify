package net.bytle.db.jdbc;

import net.bytle.db.spi.DataPath;
import net.bytle.db.spi.DataPathAbs;
import net.bytle.db.uri.DataUri;

import java.sql.DatabaseMetaData;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A jdbc data path knows only three parts
 * * catalog
 * * schema
 * * and name
 */
public class AnsiDataPath extends DataPathAbs {


  public static final String CURRENT_WORKING_DIRECTORY = ".";
  public static final String PARENT_DIRECTORY = "..";
  private static final String SEPARATOR = ".";
  private final String name;
  private final String schema;
  private final String catalog;
  private final SqlDataStore jdbcDataStore;


  /**
   * TABLE", "VIEW", "SYSTEM TABLE", "GLOBAL TEMPORARY", "LOCAL TEMPORARY", "ALIAS", "SYNONYM".
   */
  private String type = TABLE_TYPE;
  public static final String TABLE_TYPE = "TABLE";
  public static final String VIEW_TYPE = "VIEW";
  public static final String QUERY_TYPE = "QUERY";


  /**
   *
   * @param jdbcDataStore
   * @param query - a query data pth
   *
   * To facilitate creation of a SqlDataStore extension, a query data path is
   * created from a data store via {@link SqlDataStore#getQueryDataPath(String)}
   */
  protected AnsiDataPath(SqlDataStore jdbcDataStore, String query) {
    this.jdbcDataStore = jdbcDataStore;
    this.setType(QUERY_TYPE);
    this.setQuery(query);
    this.name = null;
    this.schema = jdbcDataStore.getCurrentSchema();
    this.catalog = jdbcDataStore.getCurrentCatalog();
  }


  /**
   * The global constructor for table or view.
   * Query has another one, See {@link #AnsiDataPath(SqlDataStore, String)}
   *
   * @param jdbcDataStore
   * @param catalog
   * @param schema
   * @param name
   *
   * To facilitate the SqlDataStore extension, a data path is
   * created from a data store via {@link SqlDataStore#getSqlDataPath(String, String, String)}
   */
  protected AnsiDataPath(SqlDataStore jdbcDataStore, String catalog, String schema, String name) {

    this.jdbcDataStore = jdbcDataStore;
    this.catalog = catalog;
    this.schema = schema;
    this.name = name;

  }


  @Override
  public SqlDataStore getDataStore() {
    return jdbcDataStore;
  }

  @Override
  public AnsiDataDef getOrCreateDataDef() {

    if (relationDef==null) {
      relationDef = new AnsiDataDef(this,true);
    }
    return (AnsiDataDef) relationDef;

  }

  @Override
  public AnsiDataDef createDataDef() {
    if (relationDef==null) {
      relationDef = new AnsiDataDef(this,false);
    }
    return (AnsiDataDef) relationDef;
  }


  @Override
  public DataUri getDataUri() {
    // The data URI is rebuild because the first mean of JdbcDataPath creation
    // is the JDBC API that gives catalog, schema and name

    StringBuilder stringBuilder = new StringBuilder();

    if (catalog != null) {
      stringBuilder.append(catalog).append(".");
    }
    if (schema != null) {
      stringBuilder.append(schema).append(".");
    }
    if (name != null) {
      stringBuilder.append(name).append(".");
    }
    stringBuilder.append(DataUri.AT_STRING).append(jdbcDataStore.getName());

    return DataUri.of(stringBuilder.toString());

  }

  /**
   *
   * @param name - the sibling name
   * @return a sibling of a table
   * The implementation is not complete,
   * you will never get a sibling of a catalog or a schema
   */
  @Override
  public AnsiDataPath getSibling(String name) {

    return this.getDataStore().getDataPath(catalog, schema, name);

  }

  @Override
  public AnsiDataPath getChild(String name) {
    return resolve(name);
  }

  @Override
  public AnsiDataPath resolve(String... names) {

    List<String> actualPath = new ArrayList<>();
    if (catalog != null) {
      actualPath.add(catalog);
    }
    if (schema != null) {
      actualPath.add(schema);
    }
    if (name != null) {
      actualPath.add(name);
    }
    if (names != null) {
      for (int i = 0; i < names.length; i++) {
        switch (names[i]) {
          case ".":
            // Nothing to do
            break;
          case "..":
            if (actualPath.size() == 0) {
              throw new RuntimeException("You can't apply two points on this path (..) because you are already on the root path and we can't therefore go to th parent");
            } else {
              actualPath.remove(actualPath.size() - 1);
            }
            break;
          default:
            if (names[i].startsWith("/")) {
              actualPath = new ArrayList<>();
              actualPath.add(names[i].substring(1, names[i].length() - 1));
            } else {
              actualPath.add((names[i]));
            }
        }
      }
    }
    if (this.catalog != null) {
      return this.getDataStore().getSqlDataPath(
        actualPath.size() >= 1 ? actualPath.get(0) : null,
        actualPath.size() >= 2 ? actualPath.get(1) : null,
        actualPath.size() >= 3 ? actualPath.get(2) : null);
    } else {
      if (this.schema != null) {
        return this.getDataStore().getSqlDataPath(
          null,
          actualPath.size() >= 1 ? actualPath.get(0) : null,
          actualPath.size() >= 2 ? actualPath.get(1) : null
        );
      } else {
        return this.getDataStore().getSqlDataPath(
          null,
          null,
          actualPath.size() >= 1 ? actualPath.get(0) : null
        );
      }
    }

  }

  @Override
  public DataPath getChildAsTabular(String name) {
    return getChild(name);
  }

  /**
   * {@link DatabaseMetaData#getMaxSchemaNameLength()}
   */
  public AnsiDataPath getSchema() {

    if (schema == null) {
      return null;
    } else {
      return this.getDataStore().getSqlDataPath(catalog, schema, null);
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
  public List<String> getNames() {
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
    return getNames().stream()
      .filter(n -> !n.equals(""))
      .collect(Collectors.joining(SEPARATOR));
  }


  public String getCatalog() {
    return catalog;
  }


  public boolean isDocument() {
    if (type.equals(QUERY_TYPE)) {
      return true;
    } else {
      return name != null;
    }
  }

  public AnsiDataPath setType(String type) {
    this.type = type;
    return this;
  }

  public String getType() {
    return this.type;
  }


  @Override
  public DataPath getSelectStreamDependency() {
    return null;
  }

}
