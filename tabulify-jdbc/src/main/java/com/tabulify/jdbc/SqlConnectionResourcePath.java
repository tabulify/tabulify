package com.tabulify.jdbc;

import com.tabulify.spi.ConnectionResourcePathAbs;
import com.tabulify.uri.DataUri;
import net.bytle.exception.InternalException;
import net.bytle.exception.NoCatalogException;
import net.bytle.exception.NoObjectException;
import net.bytle.exception.NoSchemaException;
import net.bytle.type.Strings;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.tabulify.jdbc.SqlMediaTypeType.*;

/**
 * This is the representation of an addressable path
 * for a sql connection
 * <p>
 * It's the path used by tabli to be able to
 * - select schema as data path
 * - use glob pattern to search sql object
 * <p>
 * You can get the sql path with {@link SqlConnectionResourcePath#toSqlStatementPath()}
 */
public class SqlConnectionResourcePath extends ConnectionResourcePathAbs {


  private final SqlConnection sqlConnection;

  /**
   * The 3 names of the data resource
   * (They may be null, the driver returns them null)
   */
  private final String catalogPart; // A shortcut to retrieve easily the catalog name
  private final String schemaPart; // A shortcut to retrieve easily the schema name
  private final String objectPart; // A shortcut to retrieve easily the schema name

  private final SqlMediaTypeType mediaType;
  private final boolean absolute;


  /**
   * @param sqlConnection   - the connection
   * @param sqlResourcePath - the tabli sql string path
   *                        <p>
   *                        A string path that is:
   *                        * null,
   *                        * the empty string
   *                        * or the {@link #getCurrentPathName()}
   *                        will use the working path.
   *                        <p>
   *                        A tabli sql string path that can be used to select, address object (catalog, schema and object name)
   *                        <p>
   *                        This is basically the same as the sql path but allows:
   *                        * empty string as name to be able to select a schema for instance
   *                        * glob character such as * and ? for selection
   */
  private SqlConnectionResourcePath(SqlConnection sqlConnection, String sqlResourcePath) {

    super(sqlResourcePath);

    Objects.requireNonNull(sqlConnection, "A working path is mandatory");


    this.sqlConnection = sqlConnection;


    /**
     * Object building
     */
    this.setPathSeparator(sqlConnection.getSeparator());
    String connectionCurrentCatalog = null;
    try {
      connectionCurrentCatalog = sqlConnection.getCurrentCatalog();
    } catch (NoCatalogException e) {
      //
    }
    String connectionCurrentSchema = sqlConnection.getCurrentSchema();
    Integer maxNames = this.sqlConnection.getMetadata().getMaxNamesInPath();

    /**
     * Init parts
     */
    if (sqlResourcePath == null || sqlResourcePath.equals(DataUri.CURRENT_CONNECTION_PATH)) {
      this.catalogPart = connectionCurrentCatalog;
      this.schemaPart = connectionCurrentSchema;
      this.objectPart = null;
      this.mediaType = SCHEMA;
      this.absolute = false;
      return;
    }


    List<String> processedPathNames = Strings.createFromString(sqlResourcePath)
      .split(this.getPathSeparator())
      .stream()
      .map(s -> sqlConnection.getDataSystem().deleteQuoteIdentifier(s))
      .collect(Collectors.toList());
    int nameSizes = processedPathNames.size();
    switch (nameSizes) {
      case 1:
        this.catalogPart = null;
        this.schemaPart = null;
        this.objectPart = processedPathNames.get(0);
        this.absolute = maxNames == 1;
        break;
      case 2:
        this.catalogPart = null;
        this.schemaPart = processedPathNames.get(0);
        this.objectPart = processedPathNames.get(1);
        this.absolute = maxNames == 2;
        break;
      case 3:
        this.catalogPart = processedPathNames.get(0);
        this.schemaPart = processedPathNames.get(1);
        this.objectPart = processedPathNames.get(2);
        this.absolute = maxNames == 3;
        break;
      default:
        throw new IllegalStateException("More than 3 names was found in the sql resource path (" + sqlResourcePath + ")");
    }

    // the type of selector
    if (this.objectPart.isEmpty()
      && this.schemaPart != null && this.schemaPart.isEmpty()
      && this.catalogPart != null
    ) {
      this.mediaType = CATALOG;
    } else if (this.objectPart.isEmpty()) {
      this.mediaType = SCHEMA;
    } else {
      this.mediaType = UNKNOWN;
    }


    if (nameSizes > sqlConnection.getMetadata().getMaxNamesInPath()) {
      throw new IllegalStateException("The stringPath (" + sqlResourcePath + ") has " + nameSizes + " parts but the connection (" + sqlConnection + ") supports only " + sqlConnection.getMetadata().getMaxNamesInPath() + " names.");
    }


  }


  public static SqlConnectionResourcePath createOfConnectionPath(SqlConnection sqlConnection, String tabliSqlPath) {
    return new SqlConnectionResourcePath(sqlConnection, tabliSqlPath);
  }

  /**
   * Create a string equivalent to the current path
   */
  public static SqlConnectionResourcePath createOfSqlDataPath(SqlDataPath sqlDataPath) {


    String catalog = null;
    String schema = null;
    String objectName = null;


    switch (sqlDataPath.getMediaType()) {
      case CATALOG:
        catalog = sqlDataPath.getLogicalName();
        break;
      case SCHEMA:
        try {
          catalog = sqlDataPath.getCatalogDataPath().getLogicalName();
        } catch (NoCatalogException e) {
          //
        }
        schema = sqlDataPath.getLogicalName();
        break;
      default:
        try {
          catalog = sqlDataPath.getCatalogDataPath().getLogicalName();
        } catch (NoCatalogException e) {
          //
        }
        try {
          schema = sqlDataPath.getSchema().getLogicalName();
        } catch (NoSchemaException e) {
          //
        }
        objectName = sqlDataPath.getLogicalName();
        break;

    }

    return SqlConnectionResourcePath.createOfCatalogSchemaAndObjectName(sqlDataPath.getConnection(), catalog, schema, objectName);

  }


  /**
   * Construct a unique representation of the stringPath
   * It will rewrite the GlobPattern to be unique.
   * <p>
   * The output will be
   * * for a catalog:    * `//.//.catalog`
   * * for a schema:     * `//.schema`
   * * for a sql object: * `catalog.schema.table` if catalog and schema are supported
   * * `schema.table`         if only the schema is supported
   * * `table`                if catalog and schema are not supported
   * <p>
   * This function will create a new SQLPath with an absolute string representation
   *
   * @return an
   */
  @Override
  public SqlConnectionResourcePath toAbsolute() {

    if (this.absolute) {
      return this;
    }

    List<String> pathsPart = new ArrayList<>();
    Integer maxNames = this.sqlConnection.getMetadata().getMaxNamesInPath();
    switch (maxNames) {
      case 3:
        try {
          pathsPart.add(this.getCatalogPartOrDefault());
          pathsPart.add(this.getSchemaPartOrDefault());
          pathsPart.add(this.getObjectPartOrNull());
        } catch (NoCatalogException e) {
          throw new InternalException("A catalog should be supported", e);
        }
        break;
      case 2:
        pathsPart.add(this.getSchemaPartOrDefault());
        pathsPart.add(this.getObjectPartOrNull());
        break;
      case 1:
        pathsPart.add(this.getObjectPartOrNull());
        break;
      default:
        throw new InternalException(maxNames + " names in a sql path is not possible");
    }

    String absolutePath = this.buildResourcePathStringFromParts(pathsPart);
    return new SqlConnectionResourcePath(this.sqlConnection, absolutePath);

  }


  /**
   * An absolute path has all names supported
   * <p>
   * If a sql system supports catalog, an absolute path
   * has all 3 parts
   */
  @Override
  public Boolean isAbsolute() {

    return absolute;

  }

  @Override
  public SqlConnectionResourcePath normalize() {

    return this;

  }

  /**
   * @param catalog the catalog of the object
   * @return a string path object
   */
  public static SqlConnectionResourcePath createOfCatalogSchemaAndObjectName(SqlConnection connection, String catalog, String schema, String objectName) {

    List<String> tabliSqlPaths = new ArrayList<>();

    // Hack: postgres supports a catalog but this is only for compliance
    // the catalog is always called postgres and the table is qualified with a schema
    if (!connection.getMetadata().supportsCatalogsInSqlStatementPath()) {
      catalog = null;
    }

    if (catalog != null) {
      tabliSqlPaths.add(catalog);
    }
    if (schema != null) {
      tabliSqlPaths.add(schema);
    } else {
      if (catalog != null) {
        tabliSqlPaths.add("");
      }
    }
    if (objectName != null) {
      tabliSqlPaths.add(objectName);
    } else {
      tabliSqlPaths.add("");
    }

    String tabliSqlStringPath = String.join(".", tabliSqlPaths);

    return new SqlConnectionResourcePath(connection, tabliSqlStringPath);
  }


  /**
   * @return a resource path where the catalog and schema have been deleted if they are the connection default
   */
  public SqlConnectionResourcePath toRelative() {

    if (!this.absolute) {
      return this;
    }

    List<String> sqlStringPaths = new ArrayList<>();

    String currentCatalog;
    try {
      currentCatalog = sqlConnection.getCurrentCatalog();
      // Implementation may return null ...
      if (currentCatalog == null) {
        currentCatalog = "";
      }
    } catch (NoCatalogException e) {
      currentCatalog = "";
    }


    switch (this.mediaType) {
      case CATALOG:
        sqlStringPaths.add(this.catalogPart);
        sqlStringPaths.add("");
        sqlStringPaths.add("");
        break;
      case SCHEMA:
        if (!currentCatalog.isEmpty() && !currentCatalog.equals(this.catalogPart)) {
          sqlStringPaths.add(this.catalogPart);
        }
        sqlStringPaths.add(this.schemaPart);
        sqlStringPaths.add("");
        break;
      default:
        if (currentCatalog != null
          && !currentCatalog.equals(DataUri.CURRENT_CONNECTION_PATH)
          && !currentCatalog.equals(this.catalogPart)) {
          sqlStringPaths.add(this.catalogPart);
        }
        String currentSchema = sqlConnection.getCurrentSchema();
        if (currentSchema != null
          && !currentSchema.equals(DataUri.CURRENT_CONNECTION_PATH)
          && !currentSchema.equals(this.schemaPart)) {
          sqlStringPaths.add(this.schemaPart);
        }
        sqlStringPaths.add(this.objectPart);
        break;

    }


    String relativePath = this.buildResourcePathStringFromParts(sqlStringPaths);

    return new SqlConnectionResourcePath(this.sqlConnection, relativePath);

  }

  /**
   * For now, we don't quote the name with {@link SqlDataSystem#createQuotedName(String)}
   * because the backward glob ref would take them
   * and that's not intended
   */
  private String buildResourcePathStringFromParts(List<String> sqlStringPaths) {
    return sqlStringPaths.stream()
      .map(s -> Objects.nonNull(s) ? s : "")
      .collect(Collectors.joining(getPathSeparator()));
  }

  public SqlMediaTypeType getSqlMediaType() {
    return this.mediaType;
  }


  public String getCatalogPart() throws NoCatalogException {
    if (this.catalogPart == null) {
      throw new NoCatalogException();
    }
    return this.catalogPart;
  }


  public String getSchemaPart() throws NoSchemaException {
    if (this.schemaPart == null || this.schemaPart.isEmpty()) {
      throw new NoSchemaException();
    }
    return this.schemaPart;
  }

  public String getObjectPart() throws NoObjectException {
    if (this.objectPart == null || this.objectPart.isEmpty()) {
      throw new NoObjectException();
    }
    return this.objectPart;
  }

  public String getCatalogPartOrDefault() throws NoCatalogException {
    try {
      return this.getCatalogPart();
    } catch (NoCatalogException e) {
      return this.sqlConnection.getCurrentCatalog();
    }
  }

  public String getSchemaPartOrDefault() {
    try {
      return this.getSchemaPart();
    } catch (NoSchemaException e) {
      return this.sqlConnection.getCurrentSchema();
    }
  }

  public String getObjectPartOrNull() {
    try {
      return this.getObjectPart();
    } catch (NoObjectException e) {
      return null;
    }
  }

  public SqlConnectionResourcePath getSibling(String sibling) {
    switch (this.getSqlMediaType()) {
      case CATALOG:
        return SqlConnectionResourcePath
          .createOfCatalogSchemaAndObjectName(this.sqlConnection,
            sibling,
            null,
            null
          );
      case SCHEMA:
        return SqlConnectionResourcePath
          .createOfCatalogSchemaAndObjectName(this.sqlConnection,
            this.catalogPart,
            sibling,
            null
          );
      case UNKNOWN:
        return SqlConnectionResourcePath
          .createOfCatalogSchemaAndObjectName(this.sqlConnection,
            this.catalogPart,
            this.schemaPart,
            sibling
          );
      default:
        throw new InternalException("The sql resource (" + this.mediaType + ") is not implemented");
    }
  }

  public SqlConnectionResourcePath resolve(String stringPath) {
    switch (this.getSqlMediaType()) {
      case CATALOG:
        return SqlConnectionResourcePath
          .createOfCatalogSchemaAndObjectName(this.sqlConnection,
            this.catalogPart,
            stringPath,
            null
          );
      case SCHEMA:
        return SqlConnectionResourcePath
          .createOfCatalogSchemaAndObjectName(this.sqlConnection,
            this.catalogPart,
            this.schemaPart,
            stringPath
          );
      case UNKNOWN:
        throw new InternalException("You can''t resolve against a object (relative path is not implemented)");
      default:
        throw new InternalException("The sql resource (" + this.mediaType + ") is not implemented");
    }
  }

  public SqlConnectionResourcePath getSchemaResourcePath() {
    return SqlConnectionResourcePath
      .createOfCatalogSchemaAndObjectName(this.sqlConnection,
        this.catalogPart,
        this.schemaPart,
        null
      );
  }

  /**
   * @return a sql name or a glob name
   */
  public String getName() {
    switch (this.getSqlMediaType()) {
      case CATALOG:
        try {
          return this.getCatalogPartOrDefault();
        } catch (NoCatalogException e) {
          throw new InternalException("Error, catalog is not supported but we have a catalog object");
        }
      case SCHEMA:
        return this.getSchemaPartOrDefault();
      default:
        try {
          return this.getObjectPart();
        } catch (NoObjectException e) {
          throw new IllegalStateException("An object should have a name");
        }
    }
  }

  public SqlConnectionResourcePath getCatalogPath() throws NoCatalogException {
    return SqlConnectionResourcePath
      .createOfCatalogSchemaAndObjectName(this.sqlConnection,
        this.getCatalogPartOrDefault(),
        null,
        null
      );
  }

  public String getCatalogPartOrDefaultOrNull() {
    try {
      return getCatalogPartOrDefault();
    } catch (NoCatalogException e) {
      return null;
    }
  }
}
