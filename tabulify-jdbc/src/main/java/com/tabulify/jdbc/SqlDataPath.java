package com.tabulify.jdbc;

import com.tabulify.spi.*;
import com.tabulify.stream.InsertStream;
import com.tabulify.stream.SelectStream;
import com.tabulify.transfer.TransferPropertiesSystem;
import com.tabulify.transfer.TransferSourceTarget;
import com.tabulify.transfer.TransferSourceTargetOrder;
import net.bytle.exception.*;
import net.bytle.type.Casts;
import net.bytle.type.MediaType;

import java.sql.DatabaseMetaData;
import java.util.List;
import java.util.Objects;

import static com.tabulify.jdbc.SqlMediaType.*;

/**
 * A jdbc data path knows only three parts
 * * catalog
 * * schema
 * * and name
 */
public class SqlDataPath extends DataPathAbs {


  /**
   * The path object representation
   */
  protected SqlConnectionResourcePath sqlConnectionResourcePath;


  @Override
  public SqlMediaType getMediaType() {

    return (SqlMediaType) this.mediaType;

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

  @Override
  public boolean hasHeaderInContent() {
    return false;
  }

  @Override
  public SchemaType getSchemaType() {
    switch ((SqlMediaType) this.mediaType) {
      case TABLE:
      case SYSTEM_TABLE:
        return SchemaType.STRICT;
      default:
        return SchemaType.LOOSE;
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
   * Request has another one, See {@link SqlRequest(SqlConnection, DataPath)}
   *
   * @param sqlConnection      the connection
   * @param compactPath        a qualified sql name or a name relative to the current connection
   * @param executableDataPath an executable data path
   */
  protected SqlDataPath(SqlConnection sqlConnection, String compactPath, DataPath executableDataPath, MediaType mediaType) {

    /**
     * An SQL path does not start from the root but from the leaf.
     *    * This function should return the given path to create it (by default a relative path. ie
     *    * mostly the name of the resource (table, view).
     * An empty path is the special root path {@link SqlMediaType.ROOT}
     */
    super(sqlConnection, compactPath, executableDataPath, mediaType);

    this.sqlConnectionResourcePath = SqlConnectionResourcePath.createOfConnectionPath(sqlConnection, compactPath);

    this.mediaType = this.buildMediaType(mediaType);


    /**
     * Add the attributes
     */
    this.addVariablesFromEnumAttributeClass(SqlDataPathAttribute.class);
    this.getOrCreateVariable(SqlDataPathAttribute.CATALOG).setValueProvider(() -> {
      try {
        return this.sqlConnectionResourcePath.getCatalogPartOrDefault();
      } catch (NoCatalogException e) {
        return null;
      }
    });
    this.getOrCreateVariable(SqlDataPathAttribute.SCHEMA).setValueProvider(this.sqlConnectionResourcePath::getSchemaPartOrDefault);
    this.getOrCreateVariable(SqlDataPathAttribute.NAME).setValueProvider(this::getName);

  }

  private SqlMediaType buildMediaType(MediaType mediaType) {

    if (mediaType != null && mediaType != OBJECT) {
      if (mediaType instanceof SqlMediaType) {
        return (SqlMediaType) mediaType;
      }
      String subType = mediaType.getSubType();
      try {
        return Casts.cast(subType, SqlMediaType.class);
      } catch (CastException e) {
        throw IllegalArgumentExceptions.createFromMessageWithPossibleValues("The sql media type (" + mediaType + ") is incorrect.", SqlMediaType.class, e);
      }
    }

    SqlMediaType resourcePathMediaType = this.sqlConnectionResourcePath.getSqlMediaType();
    if (resourcePathMediaType != OBJECT) {
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
    return this.getConnection().getDataPath(path, OBJECT);

  }

  @Override
  public SqlDataPath resolve(String name, MediaType mediaType) {
    Objects.requireNonNull(name);
    // may be null if not specified
    if (mediaType == null) {
      mediaType = OBJECT;
    }
    String path = this.getSqlConnectionResourcePath()
      .resolve(name)
      .toRelative()
      .toString();
    return this.getConnection().getDataPath(path, mediaType);
  }

  /**
   * @param stringPath the string path to resolve (there is no relative path, only the child)
   * @return the child object
   */
  @Override
  public SqlDataPath resolve(String stringPath) {

    return resolve(stringPath, null);

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

    String schemaName = this.sqlConnectionResourcePath.getSchemaResourcePath().toString();
    SqlConnection connection = this.getConnection();
    return connection.getCache().createDataPath(schemaName, SCHEMA,
      () -> new SqlDataPath(
        connection,
        schemaName,
        null, SCHEMA
      ));

  }

  @Override
  public String getLogicalName() {
    if (this.executableDataPath != null) {
      return this.executableDataPath.getLogicalName();
    }
    return super.getLogicalName();
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
    return this.getConnection().getDataSystem().getSize(this);
  }

  @Override
  public Long getCount() {

    long count = 0;

    if (Tabulars.isContainer(this)) {
      return (long) Tabulars.getChildren(this).size();
    }


    // `as count` alias is mandatory for sql server
    SqlRequest queryDataPath = this.getConnection().getRuntimeDataPath("select count(1) as count from " + this.toSqlStringPath());
    try (
      SelectStream selectStream = queryDataPath.execute().getSelectStream()
    ) {
      boolean next = selectStream.next();
      if (next) {
        count = selectStream.getInteger(1);
      }
    } catch (SelectException e) {
      boolean strict = this.getConnection().getTabular().isStrictExecution();
      String message = "Error while trying to get the count of " + this;
      if (strict) {
        throw new RuntimeException(message, e);
      } else {
        SqlLog.LOGGER_DB_JDBC.warning(message + "\n" + e.getMessage());
      }
    }
    return count;


  }

  @Override
  public InsertStream getInsertStream(DataPath source, TransferPropertiesSystem transferProperties) {
    TransferSourceTargetOrder transferSourceTarget = TransferSourceTarget.create(source, this).buildOrder(transferProperties);
    return SqlInsertStream.create(transferSourceTarget);
  }

  @Override
  public SelectStream getSelectStream() throws SelectException {
    SqlRequest sqlRequest = SqlRequest.builder()
      .setSqlObjectDataPath(this)
      .build();
    return SqlRequestExecution.executeAndGetSelectStream(sqlRequest);
  }

  public SqlDataPath getCatalogDataPath() throws NoCatalogException {
    return new SqlDataPath(
      this.getConnection(),
      this.sqlConnectionResourcePath.getCatalogPath().toString(),
      null, CATALOG
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

    return toSqlStatementPath(false);

  }

  /**
   * The qualified SQL name with its {@link SqlConnectionMetadata#getIdentifierQuote()}
   * that can be used in SQL Statement with name validation
   * Use in create statement
   */
  public String toSqlStringPathWithNameValidation() {

    return toSqlStatementPath(true);

  }

  /**
   * @return a sql string path that can be used in a sql statement (with {@link SqlConnectionMetadata#getIdentifierQuote()} quoted identifier})
   */
  private String toSqlStatementPath(Boolean nameValidation) {

    SqlConnectionResourcePath absoluteSqlConnectionResourcePath = SqlConnectionResourcePath.createOfSqlDataPath(this).toAbsolute();
    SqlConnection connection = this.getConnection();
    SqlDataSystem dataSystem = connection.getDataSystem();
    StringBuilder sqlStringPath = new StringBuilder();

    try {
      String catalogPart = absoluteSqlConnectionResourcePath.getCatalogPart();
      if (connection.getMetadata().supportsCatalogsInSqlStatementPath()) {
        String catalogName = catalogPart;
        if (nameValidation) {
          catalogName = dataSystem.validateName(catalogName);
        }
        sqlStringPath.append(dataSystem.createQuotedName(catalogName));
      }
    } catch (NoCatalogException e) {
      // no catalog
    }


    try {
      String schemaName = absoluteSqlConnectionResourcePath.getSchemaPart();
      if (sqlStringPath.length() != 0) {
        sqlStringPath.append(absoluteSqlConnectionResourcePath.getPathSeparator());
      }
      if (nameValidation) {
        schemaName = dataSystem.validateName(schemaName);
      }
      sqlStringPath.append(dataSystem.createQuotedName(schemaName));
    } catch (NoSchemaException e) {
      // No schema
    }

    // schema
    if (absoluteSqlConnectionResourcePath.getObjectPartOrNull() == null) {
      return sqlStringPath.toString();
    }

    if (sqlStringPath.length() != 0) {
      sqlStringPath.append(absoluteSqlConnectionResourcePath.getPathSeparator());
    }
    // the sql name is the logical name
    String objectName = this.getLogicalName();
    if (nameValidation) {
      objectName = dataSystem.validateName(objectName);
    }
    sqlStringPath.append(dataSystem.createQuotedName(objectName));
    return sqlStringPath.toString();

  }

  @Override
  public SqlDataPathRelationDef getRelationDef() {

    return (SqlDataPathRelationDef) super.getRelationDef();
  }

  @Override
  public SqlDataPathRelationDef createRelationDef() {
    this.relationDef = new SqlDataPathRelationDef(this, true);
    return (SqlDataPathRelationDef) this.relationDef;
  }


  @Override
  public SqlDataPathRelationDef getOrCreateRelationDef() {
    return (SqlDataPathRelationDef) super.getOrCreateRelationDef();
  }

  @Override
  public SqlDataPathRelationDef createEmptyRelationDef() {
    this.relationDef = new SqlDataPathRelationDef(this, false);
    return (SqlDataPathRelationDef) this.relationDef;
  }


  /**
   * Every sql object needs a sql statement
   * to retrieve data
   *
   * @return the sql statement script
   */
  public SqlScript getExecutableSqlScript() {
    return SqlScript.builder()
      .setSqlDataPath(this)
      .build();
  }


  public DataPath getSchemaSafe() {
    try {
      return getSchema();
    } catch (NoSchemaException e) {
      throw new InternalException("No schema known for path (" + this + ")", e);
    }
  }

}
