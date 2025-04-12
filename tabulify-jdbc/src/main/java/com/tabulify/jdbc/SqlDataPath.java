package com.tabulify.jdbc;

import com.tabulify.fs.FsDataPath;
import com.tabulify.spi.DataPath;
import com.tabulify.spi.DataPathAbs;
import com.tabulify.spi.SelectException;
import com.tabulify.spi.Tabulars;
import com.tabulify.stream.InsertStream;
import com.tabulify.stream.SelectStream;
import com.tabulify.transfer.TransferProperties;
import com.tabulify.transfer.TransferSourceTarget;
import net.bytle.exception.*;
import net.bytle.fs.Fs;
import net.bytle.type.Casts;
import net.bytle.type.Key;
import net.bytle.type.MediaType;
import net.bytle.type.Strings;

import java.sql.DatabaseMetaData;
import java.util.List;
import java.util.Objects;

import static com.tabulify.jdbc.SqlDataPathType.*;

/**
 * A jdbc data path knows only three parts
 * * catalog
 * * schema
 * * and name
 */
public class SqlDataPath extends DataPathAbs {


  private final SqlConnectionResourcePath sqlConnectionResourcePath;


  public SqlDataPath(SqlConnection sqlConnection, DataPath scriptDataPath) {

    super(sqlConnection, scriptDataPath);

    this.mediaType = SCRIPT;
    this.addVariablesFromEnumAttributeClass(SqlDataPathAttribute.class);

    String currentCatalog;
    try {
      currentCatalog = sqlConnection.getCurrentCatalog();
    } catch (NoCatalogException e) {
      currentCatalog = null;
    }
    String schema = sqlConnection.getCurrentSchema();
    /**
     * We don't read the content of the script and make a hash id
     * as the resource may not exist
     */
    String object = Key.toColumnName(scriptDataPath.getRelativePath() + "_" + scriptDataPath.getConnection() + "_" + sqlConnection);
    this.sqlConnectionResourcePath = SqlConnectionResourcePath
      .createOfCatalogSchemaAndObjectName(this.getConnection(), currentCatalog, schema, object);

    /**
     * Add variable
     */
    this.addBuildVariables();

    /**
     * Logical Name is the name of the file without the extension
     * If you load `query_1.sql`, you will create a `query_1` table
     */
    String logicalName = scriptDataPath.getLogicalName();
    if (scriptDataPath instanceof FsDataPath) {
      logicalName = Fs.getFileNameWithoutExtension(((FsDataPath) scriptDataPath).getNioPath());
    }
    this.setLogicalName(logicalName);

  }

  private void addBuildVariables() {
    this.addVariablesFromEnumAttributeClass(SqlDataPathAttribute.class);

    this.getOrCreateVariable(SqlDataPathAttribute.CATALOG).setValueProvider(() -> {
      try {
        return this.sqlConnectionResourcePath.getCatalogPartOrDefault();
      } catch (NoCatalogException e) {
        return null;
      }
    });
    this.getOrCreateVariable(SqlDataPathAttribute.SCHEMA).setValueProvider(this.sqlConnectionResourcePath::getSchemaPartOrDefault);

  }


  @Override
  public SqlDataPathType getMediaType() {

    return (SqlDataPathType) this.mediaType;

  }


  public SqlDataPath getParent() throws NoParentException {

    switch (this.getMediaType()) {
      case SCHEMA:
        try {
          return getCatalogDataPath();
        } catch (NoCatalogException e) {
          throw new NoParentException("Catalog parent are not supported");
        }
      case CATALOG:
        throw new NoParentException("The catalog (" + this + ") has no parent");
      default:
        try {
          return getSchema();
        } catch (NoSchemaException e) {
          throw new NoParentException("Schema parent are not supported");
        }
    }
  }


  /**
   * Sql Server does not allow
   * catalog name in all statement
   *
   * @param numberOfNames - the number of names (1,2)
   * @return the qualified name of the sql resources (ie  table, schema.table, ...)
   */
  public String toSqlStringPath(int numberOfNames) {

    switch (numberOfNames) {
      case 1:
        return this.getConnection().getDataSystem().createQuotedName(getName());
      case 2:
        try {
          return this.getConnection().getDataSystem().createQuotedName(getSchema().getName()) + "." + this.getConnection().getDataSystem().createQuotedName(getName());
        } catch (NoSchemaException e) {
          throw new IllegalStateException("Schema seems to be not expected but the sql path asked was of 2", e);
        }
      default:
        return toSqlStringPath();
    }

  }

  public SqlConnectionResourcePath getSqlConnectionResourcePath() {
    return this.sqlConnectionResourcePath;
  }


  /**
   * The global constructor for table or view.
   * Query has another one, See {@link #SqlDataPath(SqlConnection, DataPath)}
   *
   * @param sqlConnection the connection
   * @param path          a relative resource path from the connection
   */
  protected SqlDataPath(SqlConnection sqlConnection, String path, MediaType mediaType) {

    /**
     * An SQL path does not start from the root but from the leaf.
     *    * This function should return the given path to create it (by default a relative path. ie
     *    * mostly the name of the resource (table, view).
     * An empty path is the special root path {@link SqlDataPathType.ROOT}
     */
    super(sqlConnection, path, mediaType);

    this.sqlConnectionResourcePath = SqlConnectionResourcePath.createOfConnectionPath(sqlConnection, path);

    this.mediaType = this.buildMediaType(mediaType);


  }

  private SqlDataPathType buildMediaType(MediaType mediaType) {

    if (mediaType != null && mediaType != UNKNOWN) {
      if (mediaType instanceof SqlDataPathType) {
        return (SqlDataPathType) mediaType;
      }
      String subType = mediaType.getSubType();
      try {
        return Casts.cast(subType, SqlDataPathType.class);
      } catch (CastException e) {
        throw IllegalArgumentExceptions.createFromMessageWithPossibleValues("The sql media type (" + mediaType + ") is incorrect.", SqlDataPathType.class, e);
      }
    }

    SqlDataPathType resourcePathMediaType = this.sqlConnectionResourcePath.getSqlMediaType();
    if (resourcePathMediaType != UNKNOWN) {
      return resourcePathMediaType;
    }

    return this.getConnection().getDataSystem().getObjectMediaTypeOrDefault(
      this.sqlConnectionResourcePath.getCatalogPartOrDefaultOrNull(),
      this.sqlConnectionResourcePath.getSchemaPartOrDefault(),
      this.sqlConnectionResourcePath.getObjectPartOrNull()
    );
  }


  @Override
  public SqlConnection getConnection() {

    return (SqlConnection) super.getConnection();

  }


  /**
   * @param name - the sibling name
   * @return a sibling of a table
   * The implementation is not complete,
   * you will never get a sibling of a catalog or a schema
   */
  @Override
  public SqlDataPath getSibling(String name) {

    Objects.requireNonNull(name);
    String path = this.getSqlConnectionResourcePath().getSibling(name).toString();
    return this.getConnection().getDataPath(path, UNKNOWN);

  }

  @Override
  public SqlDataPath getChild(String name) {
    return resolve(name);
  }

  /**
   * @param stringPath the string path to resolve (there is no relative path, only the child)
   * @return the child object
   */
  @Override
  public SqlDataPath resolve(String stringPath) {

    Objects.requireNonNull(stringPath);
    String path = this.getSqlConnectionResourcePath().resolve(stringPath).toString();
    return new SqlDataPath(this.getConnection(), path, UNKNOWN);

  }

  @Override
  public DataPath getChildAsTabular(String name) {
    return getChild(name);
  }

  /**
   * @return the schema of this object
   * {@link DatabaseMetaData#getMaxSchemaNameLength()}
   * A schema or a catalog SqlDataPath does not return a schema
   * <p>
   * We throw because when searching with the SQL JDBC
   * you need to pass null and not the empty string
   */
  public SqlDataPath getSchema() throws NoSchemaException {

    return new SqlDataPath(
      this.getConnection(),
      this.sqlConnectionResourcePath.getSchemaResourcePath().toString(),
      SCHEMA
    );

  }


  /**
   * A sql data resource may be anonymous (such as a query),
   * but a name should be given
   * <p>
   * {@link DatabaseMetaData#getMaxTableNameLength()}
   */
  @Override
  public String getName() {
    return this.sqlConnectionResourcePath.getName();
  }


  @Override
  public List<String> getNames() {
    return this.sqlConnectionResourcePath.getNames();
  }


  /**
   * An absolute representation of a SCHEMA or CATALOG is relative
   * ie a SQL path starts from the leaf not from the root
   *
   * @return the absolute string path
   */
  @Override
  public String getAbsolutePath() {
    return this.sqlConnectionResourcePath.toAbsolute().toString();
  }

  @Override
  public Long getSize() {
    SqlLog.LOGGER_DB_JDBC.fine("The size operation is not yet implemented for the connection (" + this.getConnection().getProductName() + ")");
    return null;
  }

  @Override
  public Long getCount() {

    long count = 0;

    if (Tabulars.isDocument(this)) {

      DataPath queryDataPath = this.getConnection().createScriptDataPath("select count(1) from " + this.getConnection().getDataSystem().createFromClause(this));
      try (
        SelectStream selectStream = queryDataPath.getSelectStream()
      ) {
        boolean next = selectStream.next();
        if (next) {
          count = selectStream.getInteger(1);
        }
      } catch (SelectException e) {
        boolean strict = this.getConnection().getTabular().isStrict();
        String message = "Error while trying to get the count of " + this;
        if (strict) {
          throw new RuntimeException(message, e);
        } else {
          SqlLog.LOGGER_DB_JDBC.warning(message + "\n" + e.getMessage());
        }
      }
    } else {
      count = Tabulars.getChildren(this).size();
    }
    return count;
  }

  @Override
  public InsertStream getInsertStream(DataPath source, TransferProperties transferProperties) {
    TransferSourceTarget transferSourceTarget = TransferSourceTarget.create(source, this, transferProperties);
    return SqlInsertStream.create(transferSourceTarget);
  }

  @Override
  public SelectStream getSelectStream() throws SelectException {
    return new SqlSelectStream(this);
  }

  public SqlDataPath getCatalogDataPath() throws NoCatalogException {
    return new SqlDataPath(
      this.getConnection(),
      this.sqlConnectionResourcePath.getCatalogPath().toString(),
      CATALOG
    );
  }


  public boolean isDocument() {
    assert mediaType != null : "The type of data path (" + this + ") is null, we can't therefore determine if it's a document";
    return mediaType != SCHEMA && mediaType != CATALOG;
  }


  @Override
  public DataPath getSelectStreamDependency() {
    return null;
  }

  /**
   * The qualified SQL name with its {@link SqlConnectionMetadata#getIdentifierQuote()}
   * that can be used in SQL Statement
   * <p>
   * example: create view `sqlStringPath` ...
   *
   * @return the sql string path
   */
  public String toSqlStringPath() {

    return SqlConnectionResourcePath.createOfSqlDataPath(this).toAbsolute().toSqlStatementPath();

  }

  @Override
  public SqlRelationDef getRelationDef() {

    return (SqlRelationDef) super.getRelationDef();
  }

  @Override
  public SqlRelationDef createRelationDef() {
    this.relationDef = new SqlRelationDef(this, false);
    return (SqlRelationDef) this.relationDef;
  }

  /**
   * @return the script as a query definition (select)
   * See also {@link SqlDataSystem#createOrGetQuery(SqlDataPath)}
   * <p>
   * This function will remove the trailing comma.
   */
  @Override
  public String getQuery() {

    return Strings.createFromString(this.getScript().trim()).rtrim(";").toString();

  }

  @Override
  public SqlRelationDef getOrCreateRelationDef() {
    if (getRelationDef() == null) {
      this.relationDef = new SqlRelationDef(this, true);
    }
    return (SqlRelationDef) this.relationDef;
  }


}
