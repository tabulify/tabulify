package net.bytle.db.jdbc;

import net.bytle.db.model.TableDef;
import net.bytle.db.spi.DataPath;
import net.bytle.db.stream.SelectStream;
import net.bytle.db.uri.DataUri;

import java.sql.DatabaseMetaData;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A jdbc data path knows only three parts
 * * catalog
 * * schema
 * * and name
 */
public class JdbcDataPath extends DataPath {


  public static final String CURRENT_WORKING_DIRECTORY = ".";
  private static final String SEPARATOR = ".";
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


  /**
   * Constructor used in the JDBC api to build a jdbc path
   *
   * @param dataSystem
   * @param cat_name
   * @param schema_name
   * @param table_name
   * @return
   */
  public static JdbcDataPath of(JdbcDataSystem dataSystem, String cat_name, String schema_name, String table_name) {
    return new JdbcDataPath(dataSystem, cat_name, schema_name, table_name);
  }

  /**
   * Constructor from an data Uri string
   *
   * @param jdbcDataSystem
   * @param dataUri
   * @return
   */
  public static JdbcDataPath of(JdbcDataSystem jdbcDataSystem, DataUri dataUri) {

    String path = dataUri.getPath();
    List<String> pathSegments = new ArrayList<>();

    // Separator is a point by default
    char splitter = '.';
    if (path.charAt(0) == '.') {
      // except if thi
      splitter = '/';
    }
    if (path.indexOf(splitter) != -1) {
      pathSegments = Arrays.asList(path.split(String.valueOf(splitter)));
    } else {
      pathSegments.add(path);
    }

    String catalog = jdbcDataSystem.getCurrentCatalog();
    String schema = jdbcDataSystem.getCurrentSchema();
    String first = pathSegments.get(0);
    switch (first) {
      case ".":
        switch (pathSegments.size()) {
          case 1:
            return new JdbcDataPath(jdbcDataSystem, catalog, schema, null);
          case 2:
            String name = pathSegments.get(pathSegments.size() - 1);
            return new JdbcDataPath(jdbcDataSystem, catalog, schema, name);
          default:
            throw new RuntimeException("The working context is the schema and have no children, it's then not possible to have following path (" + String.join("/", pathSegments) + ")");
        }
      case "..":
        switch (pathSegments.size()) {
          case 1:
            // A catalog
            return new JdbcDataPath(jdbcDataSystem, catalog, null, null);
          case 2:
            switch (pathSegments.get(1)) {
              case "..":
                // the root
                return new JdbcDataPath(jdbcDataSystem, null, null, null);
              case ".":
                return new JdbcDataPath(jdbcDataSystem, catalog, null, null);
              default:
                schema = pathSegments.get(1);
                return new JdbcDataPath(jdbcDataSystem, catalog, schema, null);
            }
          case 3:
            schema = pathSegments.get(1);
            String name = pathSegments.get(2);
            return new JdbcDataPath(jdbcDataSystem, catalog, schema, name);
          default:
            throw new RuntimeException("A Jdbc path knows max only three parts (catalog, schema, name). This path is then not possible (" + String.join("/", pathSegments) + ")");
        }
      default:
        if (pathSegments.size() > 3) {
          throw new RuntimeException("This jdbc path (" + String.join("/", pathSegments) + ") is not a valid JDBC uri because it has more than 3 name path but a Jdbc database system supports only maximum three: catalog, schema and name");
        }

        if (pathSegments.size() > 2) {
          catalog = pathSegments.get(pathSegments.size() - 3);
        } else {
          catalog = jdbcDataSystem.getCurrentCatalog();
        }

        if (pathSegments.size() > 1) {
          schema = pathSegments.get(pathSegments.size() - 2);
        } else {
          schema = jdbcDataSystem.getCurrentSchema();
        }

        String name = pathSegments.get(pathSegments.size() - 1);
        return new JdbcDataPath(jdbcDataSystem, catalog, schema, name);

    }


  }

  public JdbcDataPath(JdbcDataSystem jdbcDataSystem, String query) {
    this.jdbcDataSystem = jdbcDataSystem;
    this.setType(QUERY_TYPE);
    this.setQuery(query);
    this.name = null;
    this.schema = jdbcDataSystem.getCurrentSchema();
    this.catalog = jdbcDataSystem.getCurrentCatalog();
  }


  /**
   * The global constructor for table or view.
   * Query has another one, See {@link #ofQuery(JdbcDataSystem, String)}
   * The data uri is not given but rebuild. See for more info {@link #getDataUri()}
   *
   * @param jdbcDataSystem
   * @param catalog
   * @param schema
   * @param name
   */
  private JdbcDataPath(JdbcDataSystem jdbcDataSystem, String catalog, String schema, String name) {
    this.jdbcDataSystem = jdbcDataSystem;
    this.catalog = catalog;
    this.schema = schema;
    this.name = name;
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
        if (this.getDataSystem().getMaxWriterConnection() == 1) {
          selectStream.close();
          selectStream = null;
        }
      } else {
        Jdbcs.getTableDef(super.getDataDef());
      }
    }
    return super.getDataDef();

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
    stringBuilder.append(DataUri.AT_STRING).append(jdbcDataSystem.getDataStore().getName());

    return DataUri.of(stringBuilder.toString());

  }

  @Override
  public DataPath getSibling(String name) {
    return new JdbcDataPath(this.jdbcDataSystem, catalog, schema, name);
  }

  @Override
  public JdbcDataPath getChild(String name) {
    return resolve(name);
  }

  @Override
  public JdbcDataPath resolve(String... names) {

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
    for (int i = 0; i < names.length; i++) {
      switch (names[i]) {
        case ".":
          // Nothing to do
          break;
        case "..":
          if (actualPath.size()==0){
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
    if (this.catalog != null) {
      return new JdbcDataPath(this.jdbcDataSystem,
        actualPath.size() >= 1 ? actualPath.get(0) : null,
        actualPath.size() >= 2 ? actualPath.get(1) : null,
        actualPath.size() >= 3 ? actualPath.get(2) : null);
    } else {
      if (this.schema != null) {
        return new JdbcDataPath(this.jdbcDataSystem,
          null,
          actualPath.size() >= 1 ? actualPath.get(0) : null,
          actualPath.size() >= 2 ? actualPath.get(1) : null
        );
      } else {
        return new JdbcDataPath(this.jdbcDataSystem,
          null,
          null,
          actualPath.size() >= 1 ? actualPath.get(0) : null
        );
      }
    }

  }

  /**
   * {@link DatabaseMetaData#getMaxSchemaNameLength()}
   */
  public JdbcDataPath getSchema() {

    if (schema == null) {
      return null;
    } else {
      return new JdbcDataPath(jdbcDataSystem, catalog, schema, null);
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
    return String.join(".", getNames());
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
    if (selectStream == null) {
      selectStream = SqlSelectStream.of(this);
    }
    return selectStream;

  }


  @Override
  public List getSelectStreamDependencies() {
    throw new RuntimeException("No select stream dependencies in jdbc");
  }
}
