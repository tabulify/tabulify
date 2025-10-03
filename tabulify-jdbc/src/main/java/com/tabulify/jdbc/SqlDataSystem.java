package com.tabulify.jdbc;

import com.tabulify.connection.Connection;
import com.tabulify.exception.DataResourceNotEmptyException;
import com.tabulify.model.*;
import com.tabulify.spi.*;
import com.tabulify.transfer.*;
import net.bytle.crypto.Digest;
import net.bytle.exception.*;
import net.bytle.regexp.Glob;
import net.bytle.type.*;

import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

import static com.tabulify.jdbc.SqlMediaType.REQUEST;
import static com.tabulify.jdbc.SqlMediaType.TABLE;
import static com.tabulify.transfer.TransferOperation.COPY;


/**
 * A object that
 * groups all SQL DML and DDL
 * operations and supporting functions
 */
public class SqlDataSystem extends DataSystemAbs {


  /**
   * The data store is needed to get the connection to be able to
   * for instance {@link #execute(List)} statements
   */
  protected SqlConnection sqlConnection;


  public SqlDataSystem(SqlConnection sqlConnection) {
    super(sqlConnection);
    this.sqlConnection = sqlConnection;
  }

  /**
   * The generation of a SQL must not be inside
   *
   * @return the select statement
   */
  public String createSelectStatement(SqlDataPath dataPath) {

    /**
     * If it does not work, "select * from " + getFullyQualifiedSqlName(dataPath); ?
     */
    return "select " +
      createColumnsStatementForQuery(dataPath.getOrCreateRelationDef().getColumnDefs()) +
      " from " +
      dataPath.toSqlStringPath();
  }

  /**
   * @return an insert into statement
   * between source and target
   */
  public String createInsertStatementWithSelect(TransferSourceTargetOrder transferSourceTarget) {

    transferSourceTarget.checkBeforeInsert();

    SqlDataPath target = (SqlDataPath) transferSourceTarget.getTargetDataPath();
    SqlDataPath source = (SqlDataPath) transferSourceTarget.getSourceDataPath();

    List<? extends ColumnDef> targetColumnsDefs = transferSourceTarget.getTargetColumnInInsertStatement();

    return "insert into " +
      target.toSqlStringPath() +
      " ( " +
      createColumnsStatementForQuery(targetColumnsDefs) +
      " ) " +
      createOrGetSelectQuery(source);

  }

  /**
   * @return a list of column name separated by a comma used in the query
   * <p>
   * Example:
   * "col1", "col2", "col3"
   */
  public String createColumnsStatementForQuery(List<? extends ColumnDef> columnDefs) {

    assert !columnDefs.isEmpty() : "The number of columns passed should be more than 0";
    SqlDataSystem dataSystem = (SqlDataSystem) columnDefs.get(0).getRelationDef().getDataPath().getConnection().getDataSystem();
    return columnDefs.stream()
      .map(col -> dataSystem.createQuotedName(col.getColumnName()))
      .collect(Collectors.joining(", "));
  }

  /**
   * Return an insert statement again the tableDef from the resultSetMetadata
   *
   * @param sqlBindVariableFormat - do we create a sql parametrized statement (ie with ?) in place of a printf format (ie %s)
   * @return an insert statement
   */
  public String createInsertStatementUtilityStatementGenerator(TransferSourceTargetOrder transferSourceTarget, Boolean sqlBindVariableFormat) {

    transferSourceTarget.checkBeforeInsert();
    return createInsertStatementUtilityValuesClauseBefore(transferSourceTarget) +
      createInsertStatementUtilityValuesClauseGenerator(transferSourceTarget, sqlBindVariableFormat, false) +
      createInsertStatementUtilityValuesClauseAfter();

  }

  /**
   * @param sqlBindVariableFormat -  if true, the sql `?` character is used, otherwise the printf pattern `%s` is used
   * @param withAlias             - if true the bind character is followed by `as columnName`
   * @return return a parametrized series of values in a sql or printf format
   */
  protected String createInsertStatementUtilityValuesClauseGenerator(TransferSourceTargetOrder transferSourceTarget, Boolean sqlBindVariableFormat, Boolean withAlias) {

    RelationDef source = transferSourceTarget.getSourceDataPath().getOrCreateRelationDef();
    StringBuilder valuesListStatement = new StringBuilder();
    for (int i = 1; i <= source.getColumnsSize(); i++) {
      ColumnDef sourceColumnDef = source.getColumnDef(i);
      ColumnDef targetColumnDef;
      try {
        targetColumnDef = transferSourceTarget.getTargetColumnFromSourceColumn(sourceColumnDef);
      } catch (NoColumnException e) {
        throw new IllegalStateException("The target column for the source column (" + sourceColumnDef + ") could not be found");
      }
      if (!targetColumnDef.isAutoincrement()) {
        if (sqlBindVariableFormat) {
          valuesListStatement.append("?");
        } else {
          valuesListStatement.append("%s");
        }
        if (withAlias) {
          valuesListStatement
            .append(" as ")
            // we quote because it's mandatory by oracle (a column without quote and with quote is the same)
            .append(createQuotedName(sourceColumnDef.getColumnName()))
          ;
        }
      }
      if (i != source.getColumnsSize()) {
        valuesListStatement.append((", "));
      }
    }
    return valuesListStatement.toString();
  }


  /**
   * The part of the insert statement after the values
   */
  protected String createInsertStatementUtilityValuesClauseAfter() {
    return " )";
  }

  public String createInsertStatementWithPrintfExpressions(TransferSourceTargetOrder transferSourceTarget) {
    return createInsertStatementUtilityStatementGenerator(transferSourceTarget, false);
  }

  /**
   * Return a parameterized insert statement again the tableDef from the resultSetMetadata
   */
  public String createInsertStatementWithBindVariables(TransferSourceTargetOrder transferSourceTarget) {

    return createInsertStatementUtilityStatementGenerator(transferSourceTarget, true);

  }

  /**
   * @return if the table exist in the underlying database (actually the letter case is important)
   * <p>
   */

  @Override
  public Boolean exists(DataPath dataPath) {


    SqlDataPath sqlDataPath = (SqlDataPath) dataPath;
    SqlMediaType sqlType = sqlDataPath.getMediaType();

    String catalog;
    try {
      catalog = sqlDataPath.getCatalogDataPath().getLogicalName();
    } catch (NoCatalogException e) {
      catalog = null;
    }

    String name = sqlDataPath.getLogicalName();
    switch (sqlType) {
      case REQUEST:
        DataPath executableDataPath = sqlDataPath.getExecutableDataPath();
        if (executableDataPath != null) {
          return Tabulars.exists(executableDataPath);
        }
        // the request is generated from a query
        return true;
      case CATALOG:
        throw new RuntimeException("Catalog exists is not yet supported");
      case SCHEMA:

        List<SqlDataPath> schemas = this.getConnection().getSchemas(catalog, name);
        switch (schemas.size()) {
          case 0:
            return false;
          case 1:
            return true;
          default:
            throw new RuntimeException("In exist, the number of schema returned should be 0 or 1. It was " + schemas.size());
        }
      default:
        if (sqlType.isRuntime()) {
          // script does not exist in the database
          // they are runtime resource
          return false;
        }
        try {

          String schema;
          try {
            schema = sqlDataPath.getSchema().getLogicalName();
          } catch (NoSchemaException e) {
            schema = null;
          }

          String[] allTypes = null; // null  means all types
          try (ResultSet tableResultSet = this.sqlConnection.getCurrentJdbcConnection().getMetaData().getTables(catalog, schema, name, allTypes)) {
            return tableResultSet.next(); // For TYPE_FORWARD_ONLY
          }

        } catch (SQLException e) {
          throw new RuntimeException(e);
        }
    }


  }

  /**
   * When names are not quoted, Oracle stores them as UPPERCASE
   * to make them case-insensitive
   * <a href="https://docs.oracle.com/en/database/oracle/oracle-database/18/sqlrf/Database-Object-Names-and-Qualifiers.html">...</a>
   *
   * @return a normalized name to use in jdbc call
   */
  protected String createNormalizedName(String name) {
    if (name == null) {
      return null;
    }
    Boolean quotingEnabled = this.getConnection().getAttribute(SqlConnectionAttributeEnum.NAME_QUOTING_ENABLED).getValueOrDefaultCastAsSafe(Boolean.class);
    if (quotingEnabled) {
      return name;
    }
    SqlNameCaseNormalization normalization = this.getConnection().getAttribute(SqlConnectionAttributeEnum.NAME_QUOTING_DISABLED_CASE).getValueOrDefaultCastAsSafe(SqlNameCaseNormalization.class);
    switch (normalization) {
      case UPPERCASE:
        return name.toUpperCase();
      case LOWERCASE:
        return name.toLowerCase();
      case NONE:
        return name;
      default:
        throw new InternalException("The name/identifier normalization casing (" + normalization + ") is unknown");
    }
  }


  @Override
  public Boolean isEmpty(DataPath dataPath) {

    return dataPath.getCount() == 0;

  }


  /**
   * When selecting a data path, you need to pass:
   * * the table name if a table
   * * the select if a select statement
   * This function returns this clause
   * You can then build a query such as:
   * * select count from fromClause
   * * select * from fromClause
   */
  protected String createFromClause(SqlDataPath sqlDataPath) {
    String fromClause = sqlDataPath.toSqlStringPath();
    if (sqlDataPath instanceof SqlRequest) {
      /**
       * The alias is mandatory for postgres
       */
      fromClause = "(" + sqlDataPath.getExecutableSqlScript().getSelect() + ") " + createQuotedName(sqlDataPath.getName());
    }
    return fromClause;
  }

  @Override
  public boolean isDocument(DataPath dataPath) {
    SqlDataPath sqlDataPath = (SqlDataPath) dataPath;
    return sqlDataPath.isDocument();
  }


  @Override
  public String getContentAsString(DataPath dataPath) {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * This is the implementation of data transfer that happens on the same data store
   * <p>
   * * A `create` table from
   * * A `insert` into from
   * * rename (move)
   */
  @Override
  public TransferListener transfer(TransferSourceTargetOrder transferOrder) {

    SqlDataPath source = (SqlDataPath) transferOrder.getSourceDataPath();
    SqlDataPath target = (SqlDataPath) transferOrder.getTargetDataPath();
    assert transferOrder.getSourceDataPath().getConnection().equals(transferOrder.getTargetDataPath().getConnection()) : "The datastore of the source (" + source.getConnection() + ") is not the same than the datastore of the target (" + target.getConnection() + ")";


    TransferPropertiesSystem transferProperties = transferOrder.getTransferProperties();

    TransferListenerAtomic transferListener = new TransferListenerAtomic(transferOrder);
    transferListener.setType(TransferType.LOCAL);


    // Check the source
    transferOrder.sourcePreChecks();
    // Special create as
    boolean useCreateAsTarget = this.canCreateAsTableCanBeUsed(transferOrder);
    // Target check
    transferOrder.targetPreOperationsAndCheck(transferListener, !useCreateAsTarget);


    // Load
    TransferOperation loadOperation = transferProperties.getOperation();
    switch (loadOperation) {
      case INSERT:
      case COPY:

        executeInsertCopy(transferOrder, transferListener, useCreateAsTarget, loadOperation);
        break;

      case UPDATE:
        String updateStatement = createUpdateStatementWithSelect(transferOrder);
        transferListener.setMethod(TransferMethod.UPDATE_FROM_QUERY);
        SqlLog.LOGGER_DB_JDBC.info("Data Transfer using the (" + transferListener.getMethod() + ") method.");
        try (Statement statement = source.getConnection().getCurrentJdbcConnection().createStatement()) {
          boolean resultSetReturned = statement.execute(updateStatement);
          if (!resultSetReturned) {
            int updateCount = statement.getUpdateCount();
            transferListener.incrementRows(updateCount);
            SqlLog.LOGGER_DB_JDBC.info(updateCount + " records where updated from (" + source + ") to (" + target + ").");
          }
          SqlLog.LOGGER_DB_JDBC.info("Data Transfer statement executed: " + Strings.createFromString(updateStatement).onOneLine().toString());
        } catch (SQLException e) {
          final String msg = "Error when executing the statement: " + updateStatement;
          SqlLog.LOGGER_DB_JDBC.severe(msg);
          transferListener.addException(e);
          throw new RuntimeException(msg, e);
        }
        break;
      case UPSERT:
        String upsertStatement = createUpsertStatementWithSelect(transferOrder);
        transferListener.setMethod(TransferMethod.UPSERT_FROM_QUERY);
        SqlLog.LOGGER_DB_JDBC.info("Data Transfer using the (" + transferListener.getMethod() + ") method");
        try (Statement statement = source.getConnection().getCurrentJdbcConnection().createStatement()) {
          boolean resultSetReturned = statement.execute(upsertStatement);
          if (!resultSetReturned) {
            int updateCount = statement.getUpdateCount();
            transferListener.incrementRows(updateCount);
            SqlLog.LOGGER_DB_JDBC.info(updateCount + " records where upsert-ed from (" + source + ") to (" + target + ")");
          }
          SqlLog.LOGGER_DB_JDBC.info("Data Transfer statement executed: " + Strings.createFromString(upsertStatement).onOneLine().toString());
        } catch (SQLException e) {
          final String msg = "Error when executing the statement: " + upsertStatement;
          SqlLog.LOGGER_DB_JDBC.severe(msg);
          transferListener.addException(e);
          throw new RuntimeException(msg, e);
        }
        break;
      case DELETE:
        String deleteStatement = createDeleteStatementWithSelect(transferOrder);
        transferListener.setMethod(TransferMethod.DELETE_FROM_QUERY);
        SqlLog.LOGGER_DB_JDBC.info("Data Transfer using the (" + transferListener.getMethod() + ") method.");
        try (Statement statement = source.getConnection().getCurrentJdbcConnection().createStatement()) {
          boolean resultSetReturned = statement.execute(deleteStatement);
          if (!resultSetReturned) {
            int deleteCount = statement.getUpdateCount();
            transferListener.incrementRows(deleteCount);
            SqlLog.LOGGER_DB_JDBC.info(deleteCount + " records where deleted from (" + source + ") to (" + target + ").");
          }
          SqlLog.LOGGER_DB_JDBC.info("Data Transfer statement executed: " + Strings.createFromString(deleteStatement).onOneLine().toString());
        } catch (SQLException e) {
          final String msg = "Error when executing the statement: " + deleteStatement;
          SqlLog.LOGGER_DB_JDBC.severe(msg);
          transferListener.addException(e);
          throw new RuntimeException(msg, e);
        }
        break;
      default:
        throw new UnsupportedOperationException("The local load operation (" + loadOperation + ") is not yet implemented");
    }

    return transferListener;

  }

  private void executeInsertCopy(TransferSourceTargetOrder transferSourceTarget, TransferListenerAtomic transferListener, boolean useCreateAsTarget, TransferOperation loadOperation) {

    SqlDataPath sqlSource = (SqlDataPath) transferSourceTarget.getSourceDataPath();
    SqlDataPath sqlTarget = (SqlDataPath) transferSourceTarget.getTargetDataPath();
    /**
     * Rename ?
     * (ie copy with source drop on the same system)
     */
    if (transferSourceTarget.isRename()) {
      String alterTableName = createAlterTableRenameStatement(sqlSource, sqlTarget);
      transferListener.setMethod(TransferMethod.RENAME);
      SqlLog.LOGGER_DB_JDBC.info("Data Transfer using the (" + transferListener.getMethod() + ") method");
      try (Statement statement = sqlSource.getConnection().getCurrentJdbcConnection().createStatement()) {
        statement.execute(alterTableName);
        SqlLog.LOGGER_DB_JDBC.info("Data Transfer statement executed: " + Strings.createFromString(alterTableName).onOneLine().toString());
      } catch (SQLException e) {
        final String msg = "Error when executing the statement: " + alterTableName;
        SqlLog.LOGGER_DB_JDBC.severe(msg);
        transferListener.addException(e);
        throw new RuntimeException(msg, e);
      }
      return;
    }

    /**
     * With statement
     */
    /**
     * Can we use a CREATE Table as ?
     */
    String copyStatement;
    if (useCreateAsTarget) {

      /**
       * Create as statement
       */
      transferListener.setMethod(TransferMethod.CREATE_TABLE_AS);

      // Statement
      copyStatement = createTableAsStatement(transferSourceTarget);
      SqlLog.LOGGER_DB_JDBC.info("Data Transfer using the `Create Table As` method");

    } else {


      // The target table should be without rows
      if (loadOperation == COPY) {
        long count = sqlTarget.getCount();
        if (count != 0) {
          throw new DataResourceNotEmptyException("In a copy operation, the target table should be empty. This is not the case. The target table (" + sqlTarget + ") has (" + count + ") rows");
        }
      }

      /**
       * Insert into from select
       */
      transferListener.setMethod(TransferMethod.INSERT_FROM_QUERY);

      /**
       * The target check is done by the manager
       * because in one transfer
       */


      // Create statement
      copyStatement = createInsertStatementWithSelect(transferSourceTarget);
      SqlLog.LOGGER_DB_JDBC.info("Data Transfer using the `Insert Query` method");


    }

    try (Statement statement = sqlSource.getConnection().getCurrentJdbcConnection().createStatement()) {
      boolean resultSetReturned = statement.execute(copyStatement);
      SqlLog.LOGGER_DB_JDBC.info("Data Transfer statement executed: " + Strings.createFromString(copyStatement).onOneLine().toString());
      if (!resultSetReturned) {
        int updateCount = statement.getUpdateCount();
        transferListener.incrementRows(updateCount);
        SqlLog.LOGGER_DB_JDBC.info(updateCount + " records were moved from (" + sqlSource + ") to (" + sqlTarget + ")");
      }
    } catch (SQLException e) {
      final String msg = "Error when executing the statement: " + copyStatement;
      SqlLog.LOGGER_DB_JDBC.severe(msg);
      transferListener.addException(e);
      throw new RuntimeException(msg, e);
    }
  }


  /**
   * @param transferSourceTarget the transfer meta
   * @return if the create table as method can be used
   */
  private boolean canCreateAsTableCanBeUsed(TransferSourceTargetOrder transferSourceTarget) {
    DataPath target = transferSourceTarget.getTargetDataPath();
    Set<TransferResourceOperations> targetOperations = transferSourceTarget.getTransferProperties().getTargetOperations();
    return (!exists(target) &&
      (
        target.getRelationDef() == null ||
          target.getRelationDef().getColumnsSize() == 0
      )
    ) || targetOperations.contains(TransferResourceOperations.DROP);
  }


  /**
   * <a href="https://www.postgresql.org/docs/current/sql-altertable.html">...</a>
   */
  private String createAlterTableRenameStatement(SqlDataPath source, SqlDataPath target) {
    return "alter table " + source.toSqlStringPath() + " rename to " + createQuotedName(target.getName());
  }

  /**
   * Create the `create table as` statement
   * <a href="https://www.postgresql.org/docs/current/sql-createtableas.html">CREATE TABLE AS</a>
   */
  protected String createTableAsStatement(TransferSourceTargetOrder transferSourceTarget) {

    SqlDataPath target = (SqlDataPath) transferSourceTarget.getTargetDataPath();
    SqlDataPath source = (SqlDataPath) transferSourceTarget.getSourceDataPath();
    return "create table " +
      target.toSqlStringPath() +
      " as " +
      createOrGetSelectQuery(source);
  }

  /**
   * Return the query if the data path is a query
   * or create the select statement if this is a table
   */
  protected String createOrGetSelectQuery(SqlDataPath source) {
    if (source instanceof SqlRequest) {
      return source.getExecutableSqlScript().getSelect();
    }
    return createSelectStatement(source);
  }


  @SuppressWarnings("unchecked")
  @Override
  public List<SqlDataPath> getDescendants(DataPath dataPath) {
    SqlDataPath sqlDataPath = (SqlDataPath) dataPath;
    String schema;
    try {
      schema = sqlDataPath.getSchema().getName();
    } catch (NoSchemaException e) {
      schema = null;
    }
    String catalog;
    try {
      catalog = sqlDataPath.getCatalogDataPath().getName();
    } catch (NoCatalogException e) {
      catalog = null;
    }
    return sqlDataPath.getConnection().getTables(catalog, schema, null);
  }


  @SuppressWarnings("unchecked")
  @Override
  public List<SqlDataPath> select(DataPath dataPath, String globNameOrPath, MediaType mediaType) {


    SqlConnectionResourcePath sqlResourcePath = SqlConnectionResourcePath.createOfConnectionPath(this.getConnection(), globNameOrPath);

    SqlMediaType type = sqlResourcePath.getSqlMediaType();
    String escapeCharacter = sqlConnection.getMetadata().getEscapeCharacter();

    Glob objectPattern = null;
    try {
      objectPattern = Glob.createOf(sqlResourcePath.getObjectPart());
    } catch (NoObjectException e) {
      //
    }

    Glob schemaPattern = null;
    try {
      schemaPattern = Glob.createOf(sqlResourcePath.getSchemaPart());
    } catch (NoSchemaException e) {
      //
    }

    Glob catalogPattern = null;
    try {
      catalogPattern = Glob.createOf(sqlResourcePath.getCatalogPart());
    } catch (NoCatalogException e) {
      //
    }
    switch (type) {
      case CATALOG:
        // catalogPattern = Glob.createOf(catalogPatternString);
        throw new UnsupportedOperationException("Catalog selection are not yet supported");
      case SCHEMA:

        if (schemaPattern == null) {
          throw new IllegalStateException("A schema pattern should be not null at this point");
        }

        if (escapeCharacter == null) {
          // Sql Matcher are not supported
          if (!schemaPattern.containsSqlMatchers() &&
            catalogPattern != null && !catalogPattern.containsSqlMatchers()
          ) {

            // No sql matchers, no need to escape
            escapeCharacter = "";

          } else {

            // SQL matchers without escape characters
            // we retrieve all schemas and filter afterwards
            List<SqlDataPath> allSchemaInCurrentCatalog = sqlConnection.getSchemas(null, null);
            Glob finalCatalogPattern1 = catalogPattern;
            Glob finalSchemaPattern = schemaPattern;
            return allSchemaInCurrentCatalog
              .stream()
              .filter(dp -> {
                Boolean matchCatalog = true;
                try {
                  SqlDataPath catalog = dp.getCatalogDataPath();
                  if (finalCatalogPattern1 != null) {
                    // Catalog supported
                    matchCatalog = finalCatalogPattern1.matches(catalog.getName());
                  }
                } catch (NoCatalogException e) {
                  // no catalog
                }
                return finalSchemaPattern.matches(dp.getName()) && matchCatalog;
              })
              .collect(Collectors.toList());

          }
        }
        String escapedCatalogPattern = null;
        if (catalogPattern != null) {
          escapedCatalogPattern = catalogPattern.toSqlPattern(escapeCharacter);
        }
        return sqlConnection.getSchemas(
          escapedCatalogPattern,
          schemaPattern.toSqlPattern(escapeCharacter)
        );
      default:

        if (objectPattern == null) {
          throw new IllegalStateException("A object pattern should be not null at this point");
        }

        /**
         * Border case when there is no escape character known
         */
        if (escapeCharacter == null) {
          if (!Glob.createOf(sqlResourcePath.toString()).containsSqlMatchers()) {

            /**
             * No Sql matchers in the glob, no need to escape,
             * we set the escape character to blank
             */
            escapeCharacter = "";

          } else {

            /**
             * The driver does not support escape character
             * One glob pattern contains Sql matchers
             * We select what we can and we filter with
             * a stream
             */

            /**
             * We try to select by passing the glob if does not contain any sql matchers
             */
            String catalogSqlPattern = catalogPattern != null ? (!catalogPattern.containsSqlMatchers() ? catalogPattern.toString() : null) : null;
            String schemaSqlPattern = schemaPattern != null ? (!schemaPattern.containsSqlMatchers() ? schemaPattern.toString() : null) : null;
            String objectSqlPattern = !objectPattern.containsSqlMatchers() ? objectPattern.toString() : null;

            /**
             * Select
             */
            List<SqlDataPath> allObjects = sqlConnection.getTables(catalogSqlPattern, schemaSqlPattern, objectSqlPattern);
            Glob finalCatalogPattern = catalogPattern;
            Glob finalSchemaPattern1 = schemaPattern;
            Glob finalObjectPattern = objectPattern;
            return allObjects
              .stream()
              .filter(dp -> {
                  String schemaName;
                  try {
                    schemaName = dp.getSchema().getName();
                  } catch (NoSchemaException e) {
                    schemaName = "";
                  }
                  String catalogName;
                  try {
                    catalogName = dp.getCatalogDataPath().getName();
                  } catch (NoCatalogException e) {
                    catalogName = "";
                  }
                  return finalObjectPattern.matches(dp.getName())
                    && (finalSchemaPattern1 != null ? finalSchemaPattern1.matches(schemaName) : true)
                    && (finalCatalogPattern != null ? finalCatalogPattern.matches(catalogName) : true);
                }
              )
              .collect(Collectors.toList());
          }

        }

        /**
         * In Sql, null means no filter
         * In Tabul, null means the default
         */
        String sqlCatalogPattern = null;
        try {
          sqlCatalogPattern = catalogPattern != null ? catalogPattern.toSqlPattern(escapeCharacter) : this.getConnection().getCurrentCatalog();
        } catch (NoCatalogException e) {
          //
        }
        String sqlSchemaPattern = schemaPattern != null ? schemaPattern.toSqlPattern(escapeCharacter) : this.getConnection().getCurrentSchema();
        String sqlTableNamePattern = objectPattern.toSqlPattern(escapeCharacter);
        return sqlConnection.getTables(sqlCatalogPattern, sqlSchemaPattern, sqlTableNamePattern);

    }

  }


  /**
   * A function that retrieve the foreign key
   * that references the primary key of the parent data path
   * (known also as exported key)
   * <p>
   * This function is a wrapper around {@link DatabaseMetaData#getExportedKeys(String, String, String)}
   *
   * @param primaryKeyDataPath - the parent data path (ie the primary table, the data resource with the primary key)
   * @return all foreign keys that reference the primary key of the parentDataPath
   */
  @Override
  public List<ForeignKeyDef> getForeignKeysThatReference(DataPath primaryKeyDataPath) {

    SqlDataPath sqlPrimaryKeyDataPath = (SqlDataPath) primaryKeyDataPath;
    final PrimaryKeyDef primaryKey = sqlPrimaryKeyDataPath.getOrCreateRelationDef().getPrimaryKey();
    if (primaryKey == null) {
      return new ArrayList<>();
    }

    String schema;
    try {
      schema = sqlPrimaryKeyDataPath.getSchema().getName();
    } catch (NoSchemaException e) {
      schema = null;
    }
    String catalog;
    try {
      catalog = sqlPrimaryKeyDataPath.getCatalogDataPath().getName();
    } catch (NoCatalogException e) {
      catalog = null;
    }
    String tableName = sqlPrimaryKeyDataPath.getName();


    /**
     * Collect the data
     */
    List<ForeignKeyDef> foreignKeyDefs = new ArrayList<>();
    List<SqlMetaForeignKey> fkDatas;
    // MySQL for instance
    if (this.sqlConnection.getMetadata().isSchemaSeenAsCatalog()) {
      catalog = schema;
      schema = null;
    }
    try (
      ResultSet tableResultSet = sqlPrimaryKeyDataPath.getConnection().getCurrentJdbcConnection().getMetaData().getExportedKeys(catalog, schema, tableName)
    ) {
      fkDatas = SqlMetaForeignKey.getForeignKeyMetaFromDriverResultSet(tableResultSet);
    } catch (SQLException e) {
      String s = "Error when getting the foreign keys that references " + primaryKeyDataPath + " (ie getExportedKeys function). Error: " + e.getMessage();
      if (primaryKeyDataPath.getConnection().getTabular().isStrictExecution()) {
        throw new RuntimeException(s, e);
      } else {
        SqlLog.LOGGER_DB_JDBC.warning(s);
        return foreignKeyDefs;
      }
    }

    /**
     * For every sqlMeta create the foreign keys
     */
    for (SqlMetaForeignKey sqlMeta : fkDatas) {

      /**
       * The catalog name may become null
       * from the driver
       */
      String primaryTableCatalogName = sqlMeta.getPrimaryTableCatalogName();
      if (
        (primaryTableCatalogName == null || primaryTableCatalogName.isEmpty())
          &&
          (catalog != null && !catalog.isEmpty())
      ) {
        primaryTableCatalogName = catalog;
      }

      SqlDataPath foreignTable = this.getConnection().createSqlDataPath(
        primaryTableCatalogName,
        sqlMeta.getForeignTableSchemaName(),
        sqlMeta.getForeignTableName()
      );
      ForeignKeyDef foreignKeyDef;
      try {
        foreignKeyDef = ForeignKeyDef.createOf(
            foreignTable.getOrCreateRelationDef(),
            primaryKey,
            sqlMeta.getForeignKeyColumns()).
          setName(sqlMeta.getName());
      } catch (Exception e) {
        throw new RuntimeException("We were unable to create a foreign key for the resource (" + primaryKeyDataPath + ") on the primary key (" + primaryKey.getColumns() + ") for the foreign resource (" + foreignTable + "). Error: " + e.getMessage(), e);
      }
      foreignKeyDefs.add(foreignKeyDef);

    }
    return foreignKeyDefs;

  }


  /**
   * Create a drop statement for a {@link Constraint}
   *
   * @throws UnsupportedOperationException when the database does not support it (example: sqlite)
   */
  protected String dropConstraintStatement(Constraint constraint) throws UnsupportedOperationException {
    StringBuilder dropConstraintStatement = new StringBuilder();
    dropConstraintStatement.append("alter ");
    SqlDataPath table = (SqlDataPath) constraint.getRelationDef().getDataPath();
    SqlMediaType type = table.getMediaType();
    //noinspection SwitchStatementWithTooFewBranches
    switch (type) {
      case TABLE:
        dropConstraintStatement.append("table ");
        break;
      default:
        throw new RuntimeException("The drop of foreign key on the table type (" + type + ") is not implemented");
    }
    dropConstraintStatement
      .append(table.toSqlStringPath())
      .append(" drop constraint ")
      .append(createQuotedName(constraint.getName()));
    return dropConstraintStatement.toString();
  }

  @Override
  public void dropConstraint(Constraint constraint) {

    String dropStatement = dropConstraintStatement(constraint);
    SqlDataPath table = (SqlDataPath) constraint.getRelationDef().getDataPath();
    /**
     * Remove in the database
     */
    try (Statement sqlStatement = table.getConnection().getCurrentJdbcConnection().createStatement()) {

      SqlLog.LOGGER_DB_JDBC.fine("Trying to drop the constraint (" + constraint.getName() + ") from the table " + table);
      sqlStatement.execute(dropStatement);
      SqlLog.LOGGER_DB_JDBC.info("Constraint (" + constraint.getName() + ") dropped.");

    } catch (SQLException e) {
      String msg = Strings.createMultiLineFromStrings("Dropping of the constraint (" + constraint + ") of the table (" + table + ") was not successful with the statement `" + dropStatement + "`"
        , "Cause: " + e.getMessage()).toString();
      SqlLog.LOGGER_DB_JDBC.severe(msg);
      throw new RuntimeException(msg, e);
    }
    /**
     * Remove in memory
     */
    if (constraint instanceof PrimaryKeyDef) {
      /**
       * Not null constraint
       */
      for (ColumnDef column : table.getOrCreateRelationDef().getPrimaryKey().getColumns()) {
        String statement = createDropNotNullConstraintStatement(column);
        execute(statement);
      }
      table.getOrCreateRelationDef().removeMetadataPrimaryKey();
    } else if (constraint instanceof UniqueKeyDef) {
      table.getOrCreateRelationDef().removeMetadataUniqueKey((UniqueKeyDef) constraint);
    }
  }


  @Override
  public boolean isContainer(DataPath dataPath) {
    return !isDocument(dataPath);
  }

  @Override
  public void create(DataPath dataPath, DataPath sourceDataPath, Map<DataPath, DataPath> sourceTargets) {

    if (!(dataPath instanceof SqlDataPath)) {
      // Internal Exception and not IllegalArgument because before calling the `create` function, the caller should check that
      throw new InternalException("The data path (" + dataPath + ") is not a sql data resource but a " + dataPath.getClass().getSimpleName());
    }
    SqlDataPath sqlTargetDataPath = (SqlDataPath) dataPath;

    SqlMediaType targetMediaType = sqlTargetDataPath.getMediaType();
    if (targetMediaType == SqlMediaType.OBJECT) {
      targetMediaType = TABLE;
    }

    switch (targetMediaType) {
      case REQUEST:
        SqlScript sqlScriptFromScript;
        if (sourceDataPath != null) {
          sqlScriptFromScript = SqlScript.builder().setExecutableDataPath(sourceDataPath).build();
        } else {
          sqlScriptFromScript = sqlTargetDataPath.getExecutableSqlScript();
        }
        this.createAsView(sqlScriptFromScript, sqlTargetDataPath);
        SqlLog.LOGGER_DB_JDBC.info("View (" + dataPath + ") created from sql script");
        return;

      case VIEW:
        // A view can be created from a select statement/create
        // sql data path
        if (sourceDataPath == null) {
          throw new InternalException("To create the view (" + sqlTargetDataPath + "), a source data resource is mandatory and was not provided");
        }
        SqlScript sqlScript = SqlScript.builder().setExecutableDataPath(sourceDataPath).build();
        this.createAsView(sqlScript, (SqlDataPath) dataPath);
        SqlLog.LOGGER_DB_JDBC.info("View (" + dataPath + ") created from query");
        return;

      case TABLE: {

        /**
         * Merge the structure and def
         */
        if (sourceDataPath != null) {
          dataPath
            .getOrCreateRelationDef()
            .mergeDataDef(sourceDataPath, sourceTargets);
        }

        // Check that the foreign tables exist
        for (ForeignKeyDef foreignKeyDef : dataPath.getOrCreateRelationDef().getForeignKeys()) {
          DataPath foreignDataPath = foreignKeyDef.getForeignPrimaryKey().getRelationDef().getDataPath();
          if (!exists(foreignDataPath)) {
            throw new RuntimeException("The foreign table (" + foreignDataPath.toString() + ") does not exist");
          }
        }

        // Create the table
        String tableStatement = createTableStatement(sqlTargetDataPath);
        this.execute(tableStatement);

        /**
         * Add the constraints
         * <p>
         * We are not throwing an error when a constraint statement is asked
         * because the CREATE statement is seen as a unit.
         * There is not a lot of change to get an constraint statement (alter)
         * without the `create` one
         * The constraint statement function are splits
         * to be able to test them
         *
         */
        // Add the Primary Key
        final PrimaryKeyDef primaryKey = dataPath.getOrCreateRelationDef().getPrimaryKey();
        if (primaryKey != null) {
          if (!primaryKey.getColumns().isEmpty()) {
            try {
              String createPrimaryKeyStatement = createPrimaryKeyStatement(sqlTargetDataPath);
              this.execute(createPrimaryKeyStatement);
            } catch (UnsupportedOperationException e) {
              // not supported
              // We don't throw as the creation may be done in the `create` table statement
              // ie sqlite
            }
          }
        }

        // Foreign key
        for (ForeignKeyDef foreignKeyDef : dataPath.getOrCreateRelationDef().getForeignKeys()) {

          try {
            String createForeignKeyStatement = createForeignKeyStatement(foreignKeyDef);
            this.execute(createForeignKeyStatement);
          } catch (UnsupportedOperationException e) {
            // not supported
            // We don't throw as the creation may be done in the `create` table statement
            // ie sqlite
          }

        }

        // Unique key
        for (UniqueKeyDef uniqueKeyDef : dataPath.getOrCreateRelationDef().getUniqueKeys()) {

          try {
            String createUniqueKeyStatement = createUniqueKeyStatement(uniqueKeyDef);
            this.execute(createUniqueKeyStatement);
          } catch (UnsupportedOperationException e) {
            // not supported
            // We don't throw as the creation may be done in the `create` table statement
            // ie sqlite
          }

        }

        SqlLog.LOGGER_DB_JDBC.info("Table (" + dataPath + ") created");

        return;
      }

      case SCHEMA:
        this.execute(createSchemaStatement(sqlTargetDataPath));
        return;
      default:

        throw new UnsupportedOperationException("The data resources (" + dataPath + ") is a " + targetMediaType + " and the creation of the kind of SQL data resource is not yet supported.");

    }

  }

  public String createSchemaStatement(SqlDataPath dataPath) {

    if (!dataPath.getMediaType().equals(SqlMediaType.SCHEMA)) {
      throw new InternalException("The data path (" + dataPath + ") is not a schema resource but a " + dataPath.getMediaType());
    }
    /**
     * MySQL requires the quote around the name
     * create schema `schema`
     */
    return "create schema " + dataPath.toSqlStringPath();

  }


  /**
   * @return a `create` statement without pk and fk
   * For a primary key, see {@link #createPrimaryKeyStatement(SqlDataPath)}
   * For a foreign key, see {@link #createForeignKeyStatement(ForeignKeyDef)}
   */
  public String createTableStatement(SqlDataPath dataPath) {

    return "create table " +
      dataPath.toSqlStringPathWithNameValidation() +
      " (\n" +
      createColumnsStatement(dataPath) +
      " )\n";

  }

  /**
   * @param sqlName - the sql name to validate
   *                throw if not valid
   */
  public String validateName(String sqlName) {

    char firstChar = sqlName.charAt(0);
    if (!String.valueOf(firstChar).matches("[a-zA-Z]")) {
      throw new IllegalArgumentException("Name (" + sqlName + ") is not valid for sql as it should start with a Latin letter (a-z, A-Z), not " + firstChar);
    }

    if (!sqlName.matches("[a-zA-Z0-9_]*")) {
      throw new IllegalArgumentException("Name (" + sqlName + ") is not valid for sql. It should contain only the following characters (a-zA-Z0-9_)");
    }

    return sqlName;
  }

  /**
   * @param dataPath : The target schema
   * @return the column string part of a `create` statement
   */
  protected String createColumnsStatement(DataPath dataPath) {


    StringBuilder statementColumnPart = new StringBuilder();
    RelationDef dataDef = dataPath.getOrCreateRelationDef();
    // columns are stored by position
    for (int i = 1; i <= dataDef.getColumnsSize(); i++) {

      try {

        ColumnDef<?> columnDef = dataDef.getColumnDef(i);
        // Add it to the columns statement
        String columnStatement = createColumnStatement(columnDef);
        statementColumnPart.append(columnStatement);

      } catch (Exception e) {

        throw new RuntimeException(e + "\nException: The Column Statement build until now for the data path (" + dataPath + ") is:\n" + statementColumnPart, e);

      }

      // Is it the end ...
      if (i != dataDef.getColumnsSize()) {
        statementColumnPart.append(",\n");
      } else {
        statementColumnPart.append("\n");
      }

    }

    return statementColumnPart.toString();

  }

  /**
   * @param columnDef - The column definition (column may be from another database)
   * @return The statement is the `create` data type statement that should be compliant
   * with the actual connection.
   */
  protected String createDataTypeStatement(ColumnDef<?> columnDef) {

    /**
     * Processing var
     */
    Connection columnSourceConnection = columnDef.getRelationDef().getDataPath().getConnection();

    /**
     * Type translation.
     * The table can come from another connection
     * For instance, from a yaml file
     */
    SqlDataType<?> targetSqlType = sqlConnection.getSqlDataTypeFromSourceColumn(columnDef);

    /**
     * Precision verification
     */
    int precision = columnDef.getPrecision();
    int maxPrecision = targetSqlType.getMaxPrecision();
    int defaultPrecision = targetSqlType.getDefaultPrecision();
    if (defaultPrecision == 0) {
      // varchar
      defaultPrecision = maxPrecision;
    }
    if (precision != 0 && maxPrecision != 0 && precision > maxPrecision) {
      String message = "The precision (" + precision + ") of the column (" + columnDef + ") is greater than the maximum allowed (" + maxPrecision + ") for the datastore (" + columnSourceConnection.getName() + ")";
      SqlLog.LOGGER_DB_JDBC.warning(message);
    }

    /**
     * Scale verification
     */
    int scale = columnDef.getScale();
    int maximumScale = targetSqlType.getMaximumScale();
    if (scale > maximumScale) {
      String message = "The scale (" + scale + ") of the column (" + columnDef + ") is greater than the maximum allowed (" + maximumScale + ") for the datastore (" + columnSourceConnection.getName() + ")";
      SqlLog.LOGGER_DB_JDBC.warning(message);
    }


    /**
     * Create the data type statement
     * See https://www.contrib.andrew.cmu.edu/~shadow/sql/sql1992.txt
     * Section 17.1 Description of SQL item descriptor areas
     * Note that we get the parent
     */
    String dataTypeCreateStatement = targetSqlType.getParentOrSelf().toKeyNormalizer().toSqlTypeCase();
    /**
     * ANSI determines the type of data, the type of statement
     * In Postgres, the `text` type has a jdbc type code of `varchar` but has no length and is more
     * a longvarchar/clob
     * May be not relying on the type code / just relying on {@link SqlDataType#getIsSpecifierMandatory() specifier} function and template
     * is the best move.
     */
    int targetTypeCode = targetSqlType.getAnsiType().getVendorTypeNumber();
    switch (targetTypeCode) {
      case Types.INTEGER:
      case Types.SMALLINT:
      case Types.TINYINT:
      case Types.REAL:
      case Types.BIGINT:
      case Types.DOUBLE:
      case Types.DATE:
      case Types.BOOLEAN:
      case Types.SQLXML:
      case Types.CLOB:
      case Types.BLOB:
      case Types.LONGVARCHAR: // clob, text mapping
      case Types.OTHER: // json
        /**
         * DataType without precision or scale (Ie they are specified by the name)
         * Example for other Jsonb or json type
         */
        return dataTypeCreateStatement;
      case Types.TIMESTAMP_WITH_TIMEZONE:
      case Types.TIME_WITH_TIMEZONE:
        String[] words = dataTypeCreateStatement.split(" ");
        String timestampWord = words[0];
        if (!(precision == 0 || precision == defaultPrecision)) {
          timestampWord = timestampWord + "(" + precision + ")";
        }
        // Example: no with time zone specifier
        // * SQL Server: datetimeoffset
        // * Postgres: timetz, timestamptz
        if (words.length == 1) {
          return timestampWord;
        }
        // We add with time zone normally
        return timestampWord + " " + Arrays.stream(words)
          .skip(1)
          .collect(Collectors.joining(" "));

      case Types.TIMESTAMP:
        // timestamp without timezone
        // timestamp precision if not specified is generally implicitly 6 (ie precision is optional)
        // https://www.postgresql.org/docs/current/datatype-datetime.html
        if (!(precision == 0 || precision == defaultPrecision)) {
          return dataTypeCreateStatement + "(" + precision + ")";
        }
        return dataTypeCreateStatement;
      case Types.TIME:
      case Types.VARCHAR:
      case Types.NVARCHAR:
      case Types.CHAR:
      case Types.NCHAR:
      case Types.BIT:
      case Types.FLOAT:
        /**
         * This data type have one parameter (ie precision/length)
         * that may be optional
         */
        if (precision == 0) {
          precision = defaultPrecision;
        }
        if (precision == defaultPrecision) {
          if (targetSqlType.getIsSpecifierMandatory() != null && targetSqlType.getIsSpecifierMandatory()) {
            return dataTypeCreateStatement + "(" + precision + ")";
          }
          return dataTypeCreateStatement;
        }
        return dataTypeCreateStatement + "(" + precision + ")";
      case Types.DECIMAL:
      case Types.NUMERIC:
        if ((precision == 0 || precision == defaultPrecision) && (scale == 0 || scale == maximumScale)) {
          return dataTypeCreateStatement;
        } else {
          if (precision == 0) {
            precision = targetSqlType.getMaxPrecision();
          }
          return dataTypeCreateStatement + "(" + precision + (scale != 0 ? "," + scale : "") + ")";
        }
      default:
        if (precision > 0) {
          return dataTypeCreateStatement + "(" + precision + (scale != 0 ? "," + scale : "") + ")";
        }
        return dataTypeCreateStatement;
    }


  }

  /**
   * @param uniqueKeyDef - The source unique key def
   * @return an alter table unique statement
   * @throws UnsupportedOperationException if not supported
   */
  protected String createUniqueKeyStatement(UniqueKeyDef uniqueKeyDef) throws UnsupportedOperationException {

    String statement = "ALTER TABLE " + ((SqlDataPath) uniqueKeyDef.getRelationDef().getDataPath()).toSqlStringPath() + " ADD ";

// The series of columns definitions (col1, col2,...)
    final List<ColumnDef<?>> columns = uniqueKeyDef.getColumns();
    List<String> columnNames = new ArrayList<>();
    for (ColumnDef<?> columnDef : columns) {
      columnNames.add(createQuotedName(columnDef.getColumnName()));
    }
    final String columnDefStatement = String.join(",", columnNames.toArray(new String[0]));

// The final statement that presence of the name
    final String name = uniqueKeyDef.getName();
    if (name == null) {
      statement = statement + "UNIQUE (" + columnDefStatement + ")";
    } else {
      statement = statement + "CONSTRAINT " + name + " UNIQUE (" + columnDefStatement + ")";
    }

    return statement;

  }


  /**
   * A blank statement in the form "columnName datatype(scale, precision)"
   * The constraint such as NOT NULL unique may change between database
   * Example Sqlite has the primary key statement before NOT NULL
   *
   * @param columnDef : The source column
   */
  protected String createColumnStatement(ColumnDef<?> columnDef) {


    String dataTypeCreateStatement = createDataTypeStatement(columnDef);

    // NOT NULL / Optionality
    String notNullStatement = "";
    PrimaryKeyDef primaryKey = columnDef.getRelationDef().getPrimaryKey();
    List<ColumnDef<?>> primaryKeyColumns = new ArrayList<>();
    if (primaryKey != null) {
      primaryKeyColumns = primaryKey.getColumns();
    }
    if (!columnDef.isNullable() || primaryKeyColumns.contains(columnDef)) {
      notNullStatement = " not null";
    }

    // Number as columnName is not possible in Oracle
    // Just with two double quote
    return createQuotedName(columnDef.getColumnName()) + " " + dataTypeCreateStatement + notNullStatement;
  }

  /**
   * Create a upsert statement to upsert data in a table
   *
   * <p>
   * Known for postgres below the term `INSERT .. ON CONFLICT .. DO UPDATE SET ..`
   * <a href="https://wiki.postgresql.org/wiki/UPSERT">...</a>
   * <a href="https://www.postgresql.org/docs/devel/sql-insert.html">sql-insert</a>
   *
   * @return a upsert statement that is used by the loader
   */
  public String createUpsertStatementWithSelect(TransferSourceTargetOrder transferSourceTarget) {


    /**
     * On postgres, the upsert statement is an insert statement
     * with an `on conflict` clause at the end
     */
    return createInsertStatementWithSelect(transferSourceTarget) + " " +
      createUpsertStatementUtilityOnConflict(transferSourceTarget);

  }

  /**
   * The `on Conflict` clause of a statement is shared with several database
   * such as Postgres, Sqlite.
   */
  protected String createUpsertStatementUtilityOnConflict(TransferSourceTargetOrder transferSourceTarget) {


    List<UniqueKeyDef> targetUniqueKeysFoundInSourceColumns = getTargetUniqueKeysFoundInSourceColumns(transferSourceTarget);

    /**
     * Build the statement
     */
    StringBuilder upsertStatement = new StringBuilder();
    if (!targetUniqueKeysFoundInSourceColumns.isEmpty()) {
      UniqueKeyDef targetUniqueKey = targetUniqueKeysFoundInSourceColumns.get(0);
      upsertStatement
        .append("on conflict ( ")
        .append(targetUniqueKey.getColumns().stream().map(ColumnDef::getColumnName).map(this::createQuotedName).collect(Collectors.joining(", ")))
        .append(" ) do update set ")
        .append(
          transferSourceTarget.getSourceNonUniqueColumnsForTarget()
            .stream()
            .map(c -> this.createQuotedName(c.getColumnName()) + " = EXCLUDED." + this.createQuotedName(c.getColumnName()))
            .collect(Collectors.joining(", ")));

    } else {

      /**
       * On conflict on primary key when there is no unique keys
       * For whatever reason, where there is no unique column, it seems mandatory
       */
      DataPath targetDataPath = transferSourceTarget.getTargetDataPath();
      PrimaryKeyDef targetPrimaryKey = targetDataPath.getOrCreateRelationDef().getPrimaryKey();
      if (targetPrimaryKey == null) {
        SqlLog.LOGGER_DB_JDBC.warning("The table (" + targetDataPath + ") has no primary key or uniques keys, the `on conflict` upsert clause was not added");
      } else {
        upsertStatement
          .append(" on conflict ( ")
          .append(targetPrimaryKey.getColumns().stream().map(ColumnDef::getColumnName).map(this::createQuotedName).collect(Collectors.joining(", ")))
          .append(" ) do update set ")
          .append(
            transferSourceTarget.getSourceNonUniqueColumnsForTarget()
              .stream()
              .map(c -> this.createQuotedName(c.getColumnName()) + " = EXCLUDED." + this.createQuotedName(c.getColumnName()))
              .collect(Collectors.joining(", ")));
      }
    }

    return upsertStatement.toString();
  }

  /**
   * Build the targetUniqueKeyFoundInSourceColumns
   * with the target unique constraint columns found in the source
   */
  protected List<UniqueKeyDef> getTargetUniqueKeysFoundInSourceColumns(TransferSourceTargetOrder transferSourceTarget) {

    List<ColumnDef<?>> uniqueColumnsForTarget = transferSourceTarget.getSourceUniqueColumnsForTarget();
    List<String> uniqueColumnsNameForTarget = uniqueColumnsForTarget.stream().map(ColumnDef::getColumnName).collect(Collectors.toList());
    SqlDataPath targetDataPath = (SqlDataPath) transferSourceTarget.getTargetDataPath();
    List<UniqueKeyDef> targetUniqueKeyFoundInSourceColumns = new ArrayList<>();
    for (UniqueKeyDef targetUniqueKey : targetDataPath.getOrCreateRelationDef().getUniqueKeys()) {
      boolean notColumnFound = false;
      for (ColumnDef<?> column : targetUniqueKey.getColumns()) {
        if (!uniqueColumnsNameForTarget.contains(column.getColumnName())) {
          notColumnFound = true;
          break;
        }
      }
      if (!notColumnFound) {
        targetUniqueKeyFoundInSourceColumns.add(targetUniqueKey);
        break;
      }
    }
    return targetUniqueKeyFoundInSourceColumns;
  }


  /**
   * @param foreignKeyDef - The source foreign key
   * @return a alter table foreign key statement
   */
  protected String createForeignKeyStatement(ForeignKeyDef foreignKeyDef) {

    SqlConnection jdbcDataSystem = (SqlConnection) foreignKeyDef.getRelationDef().getDataPath().getConnection();

    // Constraint are supported from 2.1
    // https://issues.apache.org/jira/browse/HIVE-13290
    if (jdbcDataSystem.getDatabaseName().equals(SqlConnection.DB_HIVE)) {
      if (jdbcDataSystem.getDatabaseMajorVersion() < 2) {
        return null;
      } else {
        if (jdbcDataSystem.getDatabaseMinorVersion() < 1) {
          return null;
        }
      }
    }
    StringBuilder statement = new StringBuilder().append("ALTER TABLE ");
    statement.append(((SqlDataPath) foreignKeyDef.getRelationDef().getDataPath()).toSqlStringPath())
      .append(" ADD ");
    if (foreignKeyDef.getName() != null) {
      statement.append("CONSTRAINT ")
        .append(createQuotedName(foreignKeyDef.getName()))
        .append(" ");
    }
    statement
      .append("FOREIGN KEY (");
    final List<ColumnDef<?>> nativeColumns = foreignKeyDef.getChildColumns();
    for (int i = 0; i < nativeColumns.size(); i++) {
      statement.append(createQuotedName(nativeColumns.get(i).getColumnName()));
      if (i != nativeColumns.size() - 1) {
        statement.append(", ");
      }
    }
    statement.append(") ");


    final DataPath foreignDataPath = foreignKeyDef.getForeignPrimaryKey().getRelationDef().getDataPath();
    statement
      .append("REFERENCES ")
      .append(((SqlDataPath) foreignDataPath).toSqlStringPath())
      .append(" (");
    List<ColumnDef<?>> foreignColumns = foreignDataPath.getOrCreateRelationDef().getPrimaryKey().getColumns();
    for (int i = 0; i < foreignColumns.size(); i++) {
      statement.append(createQuotedName(foreignColumns.get(i).getColumnName()));
      if (i != foreignColumns.size() - 1) {
        statement.append(", ");
      }
    }
    statement.append(")");

    return statement.toString();


  }

  protected String createPrimaryKeyStatement(SqlDataPath jdbcDataPath) {

    final PrimaryKeyDef primaryKey = jdbcDataPath.getOrCreateRelationDef().getPrimaryKey();
    if (primaryKey == null) {
      return "";
    }
    List<ColumnDef<?>> columns = primaryKey.getColumns();
    int size = columns.size();
    if (size == 0) {
      return null;
    }


    StringBuilder statement = new StringBuilder().append("ALTER TABLE ");
    statement
      .append(jdbcDataPath.toSqlStringPath())
      .append(" ADD ");
    if (primaryKey.getName() != null) {
      statement
        .append("CONSTRAINT ")
        .append(this.createQuotedName(primaryKey.getName()))
        .append(" ");
    }
    List<String> columnNames = new ArrayList<>();
    for (ColumnDef<?> columnDef : columns) {
      columnNames.add(this.createQuotedName(columnDef.getColumnName()));
    }
    statement
      .append("PRIMARY KEY  (")
      .append(String.join(", ", columnNames))
      .append(")");

    return statement.toString();
  }


  @Override
  public void drop(List<DataPath> dataPaths, Set<DropTruncateAttribute> dropAttributes) {

    /**
     * The list is already in drop order,
     * but there is a catch
     * You may have in the list (because table may depend on view)
     * * a table,
     * * a view
     * * and then a table again
     * And the drop statement is only by media types
     * Therefore, we keep the drop order with this list
     * A list of grouped data resource by media type in drop order fashion
     */
    List<Map<SqlMediaType, List<SqlDataPath>>> mediaTypeSqlDataPathsMapList = new ArrayList<>();
    SqlMediaType actualMediaType = null;
    Map<SqlMediaType, List<SqlDataPath>> actualMediaTypeSqlDataPathsMap = null;
    for (DataPath dataPath : dataPaths) {
      if (!(dataPath instanceof SqlDataPath)) {
        throw new InternalException("The data path " + dataPath + " is not a sql resource");
      }
      SqlMediaType mediaType = ((SqlDataPath) dataPath).getMediaType();
      if (actualMediaType == null || mediaType != actualMediaType) {
        actualMediaType = mediaType;
        actualMediaTypeSqlDataPathsMap = new HashMap<>();
        mediaTypeSqlDataPathsMapList.add(actualMediaTypeSqlDataPathsMap);
      }
      SqlDataPath sqlDataPath = ((SqlDataPath) dataPath);

      List<SqlDataPath> mediaTypeDataPaths = actualMediaTypeSqlDataPathsMap.computeIfAbsent(mediaType, k -> new ArrayList<>());
      mediaTypeDataPaths.add(sqlDataPath);
    }

    /**
     * Execute the drop of list grouped in drop order
     */
    for (Map<SqlMediaType, List<SqlDataPath>> mediaTypeSqlDataPathsMap : mediaTypeSqlDataPathsMapList) {
      for (Map.Entry<SqlMediaType, List<SqlDataPath>> entry : mediaTypeSqlDataPathsMap.entrySet()) {
        SqlMediaType type = entry.getKey();
        List<SqlDataPath> sqlDataPaths = entry.getValue();
        List<String> dropStatements;
        switch (type) {
          case TABLE:
          case VIEW:
          case SCHEMA:
            dropStatements = createDropStatement(sqlDataPaths, dropAttributes);
            break;
          case SYSTEM_TABLE:
          case SYSTEM_VIEW:
            // Not supported, but we don't return an error because
            // a `*@sqlite` selection returns them by default for now
            SqlLog.LOGGER_DB_JDBC.warning("The resources (" + sqlDataPaths + ") have a type of (" + type + ") and was not dropped");
            continue;
          default:
            String message = "The resource (" + sqlDataPaths + ") is not a view, a table or a schema. It's a (" + type + "). We don't support a drop on this type";
            if (this.getConnection().getTabular().isStrictExecution()) {
              throw new StrictException(message);
            }
            SqlLog.LOGGER_DB_JDBC.warning("The resources (" + sqlDataPaths + ") have a type of (" + type + ") and was not dropped");
            continue;
        }

        for (String dropStatement : dropStatements) {
          try (Statement statement = this.getConnection().getCurrentJdbcConnection().createStatement()
          ) {

            /**
             * A database Server may hang if there is uncommited transaction on a table
             * 30 second by default
             */
            statement.setQueryTimeout(30);

            SqlLog.LOGGER_DB_JDBC.fine("Trying to drop " + type + " " + dataPaths);
            statement.execute(dropStatement);
            String typeCamelCased = Strings.createFromString(type.toString()).toFirstLetterCapitalCase().toString();
            SqlLog.LOGGER_DB_JDBC.fine(typeCamelCased + " (" + dataPaths + ") dropped.");

          } catch (SQLException e) {
            String msg = Strings.createMultiLineFromStrings("Dropping of the data paths (" + sqlDataPaths + ") was not successful with the statement `" + dropStatement + "`"
              , "Cause: " + e.getMessage()).toString();
            SqlLog.LOGGER_DB_JDBC.severe(msg);
            throw new RuntimeException(msg, e);
          }
        }

        /**
         * Cache drop
         */
        SqlCache cache = this.getConnection().getCache();
        for (SqlDataPath sqlDataPath : sqlDataPaths) {
          if (dropAttributes.contains(DropTruncateAttribute.IF_EXISTS)) {
            cache.dropIfExist(sqlDataPath);
          } else {
            cache.drop(sqlDataPath);
          }
        }
      }
    }

  }


  /**
   * Drop statement is the same for table, view, schema
   */
  protected List<String> createDropStatement(List<SqlDataPath> sqlDataPaths, Set<DropTruncateAttribute> dropAttributes) {

    SqlMediaType enumObjectType = sqlDataPaths.get(0).getMediaType();
    return SqlDropStatement.builder()
      .setType(enumObjectType)
      .setIsCascadeSupported(true)
      .setIfExistsSupported(true)
      .setMultipleSqlObjectSupported(true)
      .build()
      .getStatements(sqlDataPaths, dropAttributes);

  }


  @Override
  public void truncate(List<DataPath> dataPaths, Set<DropTruncateAttribute> dropAttributes) {

    List<SqlDataPath> sqlDataPaths = new ArrayList<>();
    for (DataPath dataPath : dataPaths) {
      if (dataPath instanceof SqlDataPath) {
        SqlDataPath sqlDataPath = (SqlDataPath) dataPath;
        SqlMediaType mediaType = sqlDataPath.getMediaType();
        if (!MediaTypes.equals(mediaType, TABLE)) {
          throw new IllegalArgumentException("The data path " + dataPath + " is not a table but a " + mediaType + " and cannot be truncated.");
        }
        sqlDataPaths.add(sqlDataPath);
        continue;
      }
      throw new InternalException("The data path " + dataPath + " is not a sql resource");
    }

    /**
     * Truncating
     */
    List<String> sqls = createTruncateStatement(sqlDataPaths);
    for (String sql : sqls) {
      try (Statement statement = this.getConnection().getCurrentJdbcConnection().createStatement()) {
        //noinspection SqlSourceToSinkFlow
        statement.execute(sql);
        SqlLog.LOGGER_DB_JDBC.info("Truncate Statement executed: " + Strings.createFromString(sql).onOneLine().toString());
      } catch (SQLException e) {
        throw new RuntimeException("Error, not permitted or bad Sql:" + Strings.createFromString(sql).onOneLine().toString(), e);
      }
    }
    SqlLog.LOGGER_DB_JDBC.info("Table(s) (" + sqlDataPaths.stream().map(SqlDataPath::toSqlStringPath).collect(Collectors.joining(", ")) + ") were truncated");

  }

  /**
   * @param dataPaths - the data paths to truncate
   *                  We don't truncate them one by one now because some database may not allow it
   *                  even in order
   *                  For instance,
   *                  Caused by: org.postgresql.util.PSQLException: ERROR: cannot truncate a table referenced in a foreign key constraint
   *                  Detail: Table "f_sales" references "d_date".
   *                  Hint: Truncate table "f_sales" at the same time, or use TRUNCATE ... CASCADE.
   */
  protected List<String> createTruncateStatement(List<SqlDataPath> dataPaths) {

    // https://www.postgresql.org/docs/9.1/sql-truncate.html
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append("truncate table ");
    String tables = dataPaths.stream().map(SqlDataPath::toSqlStringPath).collect(Collectors.joining(", "));
    stringBuilder.append(tables);
    return Collections.singletonList(stringBuilder.toString());

  }


  @SuppressWarnings("unchecked")
  @Override
  public List<SqlDataPath> getChildrenDataPath(DataPath dataPath) {

    SqlDataPath sqlDataPath = (SqlDataPath) dataPath;
    SqlMediaType type = sqlDataPath.getMediaType();
    switch (type) {
      case SCHEMA:
        String schema;
        try {
          schema = sqlDataPath.getSchema().getName();
        } catch (NoSchemaException e) {
          schema = null;
        }
        String catalog;
        try {
          catalog = sqlDataPath.getCatalogDataPath().getName();
        } catch (NoCatalogException e) {
          catalog = null;
        }
        return sqlConnection.getTables(catalog, schema, null);
      case CATALOG:
        throw new UnsupportedOperationException("Getting the schemas of a catalog is not yet supported");
      default:
        throw new IllegalStateException("Getting the children of a " + type + " sql object is not possible.");
    }


  }

  /**
   * Execute SQL statements
   */
  public void execute(List<String> statements) {
    for (String statementAsString : statements) {
      execute(statementAsString);
    }
  }


  @Override
  public DataPath getTargetFromSource(DataPath sourceDataPath, MediaType mediaType, DataPath targetParentDataPath) {

    String logicalName = sourceDataPath.getLogicalName();
    String sqlName = toValidName(logicalName);
    return this.getConnection().getDataPath(sqlName, mediaType);

  }

  @Override
  public String toValidName(String name) {
    return SqlName.create(name).toValidSqlName();
  }

  /**
   * Execute a sql statement
   */
  public void execute(String statement) {
    try (Statement sqlStatement = this.getConnection().getCurrentJdbcConnection().createStatement()) {
      boolean resultAsResultSet = sqlStatement.execute(statement);
      if (resultAsResultSet) {
        throw new IllegalArgumentException("The statement is not an executable script but a query as it returns a result set");
      }
    } catch (SQLException e) {
      String message = Strings.createMultiLineFromStrings(
        "\nError: ",
        "",
        e.getMessage(),
        "",
        "This error occurred executing the following statement:",
        "",
        statement).toString();
      /**
       * This is at a fine level because we
       * try to create view to determine the columns,
       * and it may fail but this is not an error or even an info
       */
      SqlLog.LOGGER_DB_JDBC.fine(message);
      throw new RuntimeException(message, e);
    }
  }

  public SqlConnection getConnection() {
    return sqlConnection;
  }


  /**
   * A utility function to quote a SQL identifier
   * For example: 5f110ee2 is not valid as it starts with a number but "5f110ee2" is
   * <p>
   * {@link DatabaseMetaData#getIdentifierQuoteString()}
   *
   * @param word - the word to quote
   */
  public String createQuotedName(String word) {

    Boolean quotingEnabled = this.getConnection().getAttribute(SqlConnectionAttributeEnum.NAME_QUOTING_ENABLED).getValueOrDefaultCastAsSafe(Boolean.class);
    if (!quotingEnabled) {
      return createNormalizedName(word);
    }

    String identifierQuoteString = sqlConnection.getMetadata().getIdentifierQuote();
    return identifierQuoteString + word + identifierQuoteString;


  }

  /**
   * Build the metadata type
   * (by default from the driver ie {@link DatabaseMetaData#getTypeInfo()}
   * For type returned by table, see {@link #getMetaColumns(SqlDataPath)}
   * Link: <a href="https://docs.oracle.com/javase/8/docs/api/java/sql/DatabaseMetaData.html#getTypeInfo--">...</a>
   */
  @Override
  public void dataTypeBuildingMain(SqlDataTypeManager sqlDataTypeManager) {

    try {
      ResultSet typeInfoResultSet = sqlConnection.getCurrentJdbcConnection().getMetaData().getTypeInfo();

      /**
       * For the same type code, you may get more than one line
       * Postgres for instance send the aliases this way
       */
      while (typeInfoResultSet.next()) {

        String typeNameString = typeInfoResultSet.getString("TYPE_NAME");
        int typeCode = typeInfoResultSet.getInt("DATA_TYPE");
        if (typeCode == Types.ARRAY) {
          /**
           * We don't support array type
           * Postgres returns for an array of varchar the name `_varchar`
           * Because for us this 2 names are equals, that's a lot of hell to support
           */
          continue;
        }
        KeyNormalizer typeName = KeyNormalizer.createSafe(typeNameString);

        /**
         * Get the vendor type to retrieve the class
         */
        SqlDataTypeVendor vendorType = this.getSqlDataTypeVendor(typeName, typeCode);
        KeyNormalizer principalName = typeName;
        if (!(vendorType instanceof SqlDataTypeAnsi)) {
          /**
           * We take the principal name (the type name may be an alias)
           * For instance, int4 in Postgres is used in the driver but the public name in information schema is integer.
           * This way, we create first the normalized name (ie integer for Postgres)
           * and the alias relation is created last (See below Override with vendor info)
           */
          principalName = vendorType.toKeyNormalizer();
        }
        SqlDataTypeKey sqlDataTypeKey = new SqlDataTypeKey(getConnection(), principalName, typeCode);

        /**
         * Init the builder
         */
        Class<?> valueClass = vendorType.getValueClass();
        if (valueClass == SqlDataTypeAnsi.class) {
          throw new InternalException("The class of the vendor type (" + vendorType + ") is an ansi class. Did you use the method (getClass) instead of (getValueClass)");
        }
        SqlDataType.SqlDataTypeBuilder<?> typeBuilder = sqlDataTypeManager.createTypeBuilder(sqlDataTypeKey, valueClass);

        int precision = typeInfoResultSet.getInt("PRECISION");
        typeBuilder.setMaxPrecision(precision);
        String literalPrefix = typeInfoResultSet.getString("LITERAL_PREFIX");
        typeBuilder.setLiteralPrefix(literalPrefix);
        String literalSuffix = typeInfoResultSet.getString("LITERAL_SUFFIX");
        typeBuilder.setLiteralSuffix(literalSuffix);
        String createParams = typeInfoResultSet.getString("CREATE_PARAMS");
        typeBuilder.setCreateParams(createParams);
        int nullable = typeInfoResultSet.getInt("NULLABLE");
        typeBuilder.setNullable(SqlDataTypeNullable.cast(nullable));
        Boolean caseSensitive = typeInfoResultSet.getBoolean("CASE_SENSITIVE");
        typeBuilder.setCaseSensitive(caseSensitive);
        Short searchable = typeInfoResultSet.getShort("SEARCHABLE");
        typeBuilder.setSearchable(searchable);
        Boolean unsignedAttribute = typeInfoResultSet.getBoolean("UNSIGNED_ATTRIBUTE");
        typeBuilder.setUnsignedAttribute(unsignedAttribute);
        Boolean fixedPrecisionScale = typeInfoResultSet.getBoolean("FIXED_PREC_SCALE");
        typeBuilder.setIsFixedPrecisionScale(fixedPrecisionScale);
        Boolean autoIncrement = typeInfoResultSet.getBoolean("AUTO_INCREMENT");
        typeBuilder.setAutoIncrement(autoIncrement);
        String localTypeName = typeInfoResultSet.getString("LOCAL_TYPE_NAME");
        typeBuilder.setLocalTypeName(localTypeName);
        short minimumScale = typeInfoResultSet.getShort("MINIMUM_SCALE");
        typeBuilder.setMinimumScale(minimumScale);
        short maximumScale = typeInfoResultSet.getShort("MAXIMUM_SCALE");
        typeBuilder.setMaximumScale(maximumScale);

        /**
         * Override with vendor info
         * only if it's not our own
         */
        if (vendorType instanceof SqlDataTypeAnsi) {
          typeBuilder.setAnsiType((SqlDataTypeAnsi) vendorType);
        } else {
          /**
           * Override with vendor info
           */
          typeBuilder.setDescription(vendorType.getDescription());
          if (vendorType.getAnsiType() != null) {
            typeBuilder.setAnsiType(vendorType.getAnsiType());
          }
          int maximumScaleVendor = vendorType.getMaximumScale();
          if (maximumScaleVendor != 0) {
            int maxScaleDriver = typeBuilder.getMaximumScale();
            if (maximumScaleVendor > maxScaleDriver) {
              typeBuilder.setMaximumScale(maximumScaleVendor);
            }
          }
          int minimumScaleVendor = vendorType.getMinScale();
          if (minimumScaleVendor != 0) {
            int minimumScaleDriver = typeBuilder.getMinimumScale();
            if (minimumScaleVendor < minimumScaleDriver) {
              typeBuilder.setMaximumScale(minimumScaleVendor);
            }
          }
          int maxPrecisionVendor = vendorType.getMaxPrecision();
          if (maxPrecisionVendor != 0) {
            int maxPrecisionDriver = typeBuilder.getMaxPrecision();
            if (maxPrecisionVendor > maxPrecisionDriver) {
              typeBuilder.setMaxPrecision(maxPrecisionVendor);
            }
          }
          int defaultPrecisionVendor = vendorType.getDefaultPrecision();
          if (defaultPrecisionVendor != 0) {
            typeBuilder.setDefaultPrecision(defaultPrecisionVendor);
          }
          List<KeyInterface> vendorAliases = vendorType.getAliases();
          for (KeyInterface keyInterface : vendorAliases) {
            if (keyInterface instanceof SqlDataTypeAnsi) {
              typeBuilder.addChildAliasTypedName((SqlDataTypeAnsi) keyInterface, sqlDataTypeManager);
              continue;
            }
            typeBuilder.addChildAliasName(keyInterface);
          }
        }
      }

    } catch (SQLException e) {
      throw new RuntimeException(e);
    }


  }


  /**
   * The metadata read from columns
   *
   * @param dataPath - the data path
   * @return the list of meta columns ordered by position (asc)
   * For type returned by the driver, see {@link #dataTypeBuildingMain(SqlDataTypeManager)}
   * Wrapper over <a href="https://docs.oracle.com/javase/8/docs/api/java/sql/DatabaseMetaData.html#getColumns-java.lang.String-java.lang.String-java.lang.String-java.lang.String-">getColumns meta function</a>
   */
  public List<SqlMetaColumn> getMetaColumns(SqlDataPath dataPath) {

    List<SqlMetaColumn> sqlMetaColumns = new ArrayList<>();
    String schemaName;
    try {
      schemaName = dataPath.getSchema().getName();
    } catch (NoSchemaException e) {
      schemaName = null;
    }

    String catalog;
    try {
      catalog = dataPath.getCatalogDataPath().getName();
    } catch (NoCatalogException e) {
      catalog = null;
    }

    if (this.getConnection().getMetadata().isSchemaSeenAsCatalog()) {
      catalog = schemaName;
      schemaName = null;
    }

    try (
      ResultSet columnResultSet = dataPath.getConnection().getCurrentJdbcConnection().getMetaData().getColumns(catalog, schemaName, dataPath.getName(), null)
    ) {
      while (columnResultSet.next()) {

        String columnName = columnResultSet.getString("COLUMN_NAME");
        SqlMetaColumn meta = SqlMetaColumn.createOf(columnName);
        sqlMetaColumns.add(meta);

        // Not implemented on all driver (example: sqliteDriver)
        try {
          meta.setIsGeneratedColumn(jdbcBooleanCast(columnResultSet.getString("IS_GENERATEDCOLUMN")));
        } catch (SQLException e) {
          SqlLog.LOGGER_DB_JDBC.fine("The IS_GENERATEDCOLUMN column seems not to be implemented. Message: " + e.getMessage());
        }

        // Not implemented on all driver (example: sqliteDriver)
        try {

          String isAutoincrement = columnResultSet.getString("IS_AUTOINCREMENT").toLowerCase();
          meta.setIsAutoIncrement(jdbcBooleanCast(isAutoincrement));


        } catch (SQLException e) {
          SqlLog.LOGGER_DB_JDBC.fine("The IS_AUTOINCREMENT column seems not to be implemented. Message: " + e.getMessage());
        }

        int typeCode = columnResultSet.getInt("DATA_TYPE");

        // The display size of the column on the terminal
        // (COLUMN_SIZE definition)
        // As specified in the doc https://docs.oracle.com/javase/8/docs/api/java/sql/DatabaseMetaData.html#getColumns-java.lang.String-java.lang.String-java.lang.String-java.lang.String-
        // * For numeric data, this is the maximum precision.
        // * For character data, this is the length in characters.
        // * For datetime datatypes, this is the length in characters
        int columnSize = columnResultSet.getInt("COLUMN_SIZE");
        // BUFFER_LENGTH not used
        int decimalDigits = columnResultSet.getInt("DECIMAL_DIGITS");


        String typeName = columnResultSet.getString("TYPE_NAME");
        String comment = columnResultSet.getString("REMARKS");
        int isNullable = columnResultSet.getInt("NULLABLE");
        meta
          .setColumnSize(columnSize)
          .setTypeCode(typeCode)
          .setTypeName(typeName)
          .setDecimalDigits(decimalDigits)
          .setIsNullable(isNullable)
          .setComment(comment);
      }

    } catch (
      SQLException e) {
      throw new RuntimeException(e);
    }
    return sqlMetaColumns;
  }

  /**
   * Cast the `Is` value of
   * <a href="https://docs.oracle.com/javase/8/docs/api/java/sql/DatabaseMetaData.html#getColumns-java.lang.String-java.lang.String-java.lang.String-java.lang.String-">getColumns</a>
   *
   * @return true/false or null
   * JDBC metadata
   * YES -> true
   * NO -> false
   * '' -> Empty string: not known -> null
   */
  private Boolean jdbcBooleanCast(String booleanValue) {
    switch (booleanValue.toLowerCase()) {
      case "yes":
        return true;
      case "no":
        return false;
      case "":
        return null;
      default:
        throw new RuntimeException("value (" + booleanValue + ") is not JDBC column boolean value");
    }
  }


  /**
   * Return a update statement
   * the target should have a primary key
   * and the source should have a column name
   * with the same name than the primary key
   * <p>
   * Column Mapping is done by name
   * <p>
   * Adapted from the example
   * `UPDATE accounts SET (contact_first_name, contact_last_name) = `
   * at the <a href="https://www.postgresql.org/docs/9.5/sql-update.html">following page</a>
   * Why ? because this query form is guaranteed to raise an error if there are multiple id matches.
   */
  public String createUpdateStatementWithSelect(TransferSourceTargetOrder transferSourceTarget) {

    SqlDataPath source = (SqlDataPath) transferSourceTarget.getSourceDataPath();

    /**
     * First part of the select
     */
    StringBuilder update = new StringBuilder();
    update
      .append(createUpdateStatementUtilityFirstPartUntilSet(transferSourceTarget))
      .append(" ( ");

    /**
     * Add columns
     */
    Set<String> columnsInSet = transferSourceTarget.getSourceColumnsInUpdateSetClause()
      .stream()
      .map(ColumnDef::getColumnName)
      .map(this::createQuotedName)
      .collect(Collectors.toSet());

    update
      .append(String.join(", ", columnsInSet))
      .append(" ) = ( select ")
      .append(String.join(", ", columnsInSet));

    /**
     * From
     */
    update.append(" from ");
    if (source.getMediaType() == REQUEST) {
      update.append("( ")
        .append(source.getExecutableSqlScript().getSelect())
        .append(" ) ")
        .append(createQuotedName(source.getLogicalName()));
    } else {
      update
        .append(source.toSqlStringPath())
        .append(" ")
        .append(createQuotedName(source.getLogicalName()));
    }

    /**
     * Where Clause
     * Get the target unique column
     *      * present in the source
     *      * that are used in the where clause
     */
    Set<String> sourceUniqueColumns = transferSourceTarget.getSourceUniqueColumnsForTarget()
      .stream()
      .map(ColumnDef::getColumnName)
      .collect(Collectors.toSet());
    update.append(" where ");
    String whereClause = sourceUniqueColumns.stream()
      .map(uniqueColumnName -> createQuotedName(transferSourceTarget.getTargetAlias()) + "." + createQuotedName(uniqueColumnName) + " = " + createQuotedName(source.getLogicalName()) + "." + createQuotedName(uniqueColumnName))
      .collect(Collectors.joining(" and "));
    update.append(whereClause)
      .append(" )");

    return update.toString();
  }

  /**
   * Build the first clause of an update statement
   * (ie from `update` ... to `set (` )
   */
  private String createUpdateStatementUtilityFirstPartUntilSet(TransferSourceTargetOrder transferSourceTarget) {
    /**
     * Build the update statement
     */
    return "update " +
      ((SqlDataPath) transferSourceTarget.getTargetDataPath()).toSqlStringPath() +
      " as " +
      transferSourceTarget.getTargetAlias() +
      " set";
  }


  @Override
  public void dropNotNullConstraint(DataPath dataPath) {
    for (ColumnDef<?> columnDef : dataPath.getOrCreateRelationDef().getColumnDefs()) {
      if (
        // if the column is not nullable
        !columnDef.isNullable()
          && (
          // this is not a primary key column
          dataPath.getRelationDef().getPrimaryKey() != null
            && !dataPath.getRelationDef().getPrimaryKey().getColumns().contains(columnDef)
        )
      ) {
        columnDef.setNullable(true);
        String statement = createDropNotNullConstraintStatement(columnDef);
        execute(statement);
      }
    }
  }

  /**
   * Create the drop not null constraint statement
   * <p>
   * From <a href="https://www.postgresql.org/docs/9.5/ddl-alter.html">...</a>
   */
  protected String createDropNotNullConstraintStatement(ColumnDef<?> columnDef) {
    SqlDataPath sqlDataPath = (SqlDataPath) columnDef.getRelationDef().getDataPath();
    return "alter table " + sqlDataPath.toSqlStringPath() + " alter column " + createQuotedName(columnDef.getColumnName()) + " drop not null";
  }

  public TransferOperation getDefaultTransferOperation() {
    return TransferOperation.INSERT;
  }

  /**
   * Create upsert from values statement
   */
  public String createUpsertMergeStatementWithPrintfExpressions(TransferSourceTargetOrder transferSourceTarget) {
    return null;
  }

  public String createUpsertMergeStatementWithParameters(TransferSourceTargetOrder transferSourceTarget) {
    return null;
  }

  /**
   * The insert statement before the values
   * The insert statement is split in two to not build it again on every insert
   */
  public String createInsertStatementUtilityValuesClauseBefore(TransferSourceTargetOrder transferSourceTarget) {

    transferSourceTarget.checkBeforeInsert();

    RelationDef target = transferSourceTarget.getTargetDataPath().getOrCreateRelationDef();
    final SqlDataPath dataPath = (SqlDataPath) target.getDataPath();
    StringBuilder insertStatement = new StringBuilder();
    insertStatement.append("insert into ")
      .append(dataPath.toSqlStringPath())
      .append(" ( ");

    List<? extends ColumnDef<?>> targetColumnsToLoad = transferSourceTarget.getTargetColumnInInsertStatement();
    for (int i = 0; i < targetColumnsToLoad.size(); i++) {
      ColumnDef columnDefToLoad = targetColumnsToLoad.get(i);
      if (!columnDefToLoad.isAutoincrement()) {
        insertStatement.append(this.sqlConnection.getDataSystem().createQuotedName(columnDefToLoad.getColumnName()));
      }
      if (i != targetColumnsToLoad.size() - 1) {
        insertStatement.append(", ");
      }
    }

    insertStatement.append(" ) values ( ");
    return insertStatement.toString();
  }

  /**
   * @return the first of a upsert with values statement just before that the values are given
   * <p>
   * Because a upsert statement with values should be rewritten each time, the sql statement part
   * that are before and after the values are computed only once
   */
  public String createUpsertStatementUtilityValuesPartBefore(TransferSourceTargetOrder transferSourceTarget) {
    return createInsertStatementUtilityValuesClauseBefore(transferSourceTarget);
  }

  /**
   * @return the last part of a upsert with values statement just after that the values are given
   * <p>
   * Because a upsert statement with values should be rewritten each time, the sql statement part
   * that are before and after the values are computed only once
   */
  public String createUpsertStatementUtilityValuesPartAfter(TransferSourceTargetOrder transferSourceTarget) {
    return createInsertStatementUtilityValuesClauseAfter() + " " +
      createUpsertStatementUtilityOnConflict(transferSourceTarget);
  }

  /**
   * @return a update statement with Sql bind variable if sqlBindVariable is true, otherwise with a printf expression
   */
  protected String createUpdateStatementUtilityStatementGenerator(TransferSourceTargetOrder transferSourceTarget, Boolean sqlBindVariable) {
    /**
     * Start of the update
     */
    StringBuilder update = new StringBuilder();
    update.append(createUpdateStatementUtilityFirstPartUntilSet(transferSourceTarget));

    /**
     * Value clause
     */
    String valueClauseWithVariableBinding = transferSourceTarget.getSourceColumnsInUpdateSetClause()
      .stream()
      .map(c -> {
        String exp = c.getColumnName() + " = ";
        if (sqlBindVariable) {
          return exp + "?";
        } else {
          return exp + "%s";
        }
      })
      .collect(Collectors.joining(", "));
    update
      .append(" ")
      .append(valueClauseWithVariableBinding);
    /**
     * Where
     */
    String whereClauseWithVariableBinding = transferSourceTarget.getSourceUniqueColumnsForTarget()
      .stream()
      .sorted()
      .map(c -> {
        String exp = c.getColumnName() + " = ";
        if (sqlBindVariable) {
          return exp + "?";
        } else {
          return exp + "%s";
        }
      })
      .collect(Collectors.joining(" and "));
    update
      .append(" where ")
      .append(whereClauseWithVariableBinding);
    return update.toString();
  }

  public String createUpdateStatementWithBindVariables(TransferSourceTargetOrder transferSourceTarget) {

    return createUpdateStatementUtilityStatementGenerator(transferSourceTarget, true);
  }


  public String createUpdateStatementWithPrintfExpressions(TransferSourceTargetOrder transferSourceTarget) {
    return createUpdateStatementUtilityStatementGenerator(transferSourceTarget, false);
  }

  /**
   * <a href="https://www.postgresql.org/docs/10/sql-delete.html">...</a>
   */
  public String createDeleteStatementWithSelect(TransferSourceTargetOrder transferSourceTarget) {
    /**
     * Start of the statement
     */
    transferSourceTarget.checkBeforeDelete();
    StringBuilder delete = new StringBuilder();
    SqlDataPath targetDataPath = (SqlDataPath) transferSourceTarget.getTargetDataPath();
    delete.append("delete from ")
      .append(targetDataPath.toSqlStringPath())
      .append(" where ");

    String uniqueColumnsClause = transferSourceTarget.getSourceUniqueColumnsForTarget()
      .stream()
      .sorted()
      .map(ColumnDef::getColumnName)
      .map(this::createQuotedName)
      .collect(Collectors.joining(", "));
    delete.append("( ")
      .append(uniqueColumnsClause)
      // distinct was needed by sqlite
      .append(" ) in ( select distinct ")
      .append(uniqueColumnsClause)
      .append(" from ");

    SqlDataPath sourceDataPath = (SqlDataPath) transferSourceTarget.getSourceDataPath();
    SqlMediaType enumObjectType = sourceDataPath.getMediaType();
    switch (enumObjectType) {
      case TABLE:
        delete
          .append(sourceDataPath.toSqlStringPath())
          .append(" )");
        break;
      case REQUEST:
        delete
          .append("( ")
          .append(sourceDataPath.getExecutableSqlScript().getSelect())
          .append(" ) ")
          .append(createQuotedName(sourceDataPath.getLogicalName()))
          .append(" )");
        break;
      default:
        throw new UnsupportedOperationException("The creation of a SQL delete statement is not supported for a data path with the type (" + enumObjectType + ")");
    }
    return delete.toString();
  }

  public String createDeleteStatementWithPrintfExpressions(TransferSourceTargetOrder transferSourceTarget) {
    return createDeleteStatementUtilityStatementGenerator(transferSourceTarget, false);
  }

  public String createDeleteStatementWithBindVariables(TransferSourceTargetOrder transferSourceTarget) {
    return createDeleteStatementUtilityStatementGenerator(transferSourceTarget, true);
  }

  private String createDeleteStatementUtilityStatementGenerator(TransferSourceTargetOrder transferSourceTarget, boolean sqlBindVariable) {
    transferSourceTarget.checkBeforeDelete();
    StringBuilder delete = new StringBuilder();
    SqlDataPath targetDataPath = (SqlDataPath) transferSourceTarget.getTargetDataPath();
    delete.append("delete from ")
      .append(targetDataPath.toSqlStringPath())
      .append(" where ");
    String uniqueColumnsWhereClause = transferSourceTarget.getSourceUniqueColumnsForTarget()
      .stream()
      .sorted()
      .map(c -> {
        String exp = this.createQuotedName(c.getColumnName()) + " = ";
        if (sqlBindVariable) {
          return exp + "?";
        } else {
          return exp + "%s";
        }
      })
      .collect(Collectors.joining(" and "));
    delete.append(uniqueColumnsWhereClause);
    return delete.toString();
  }

  public List<SqlMetaForeignKey> getMetaForeignKeys(SqlDataPath dataPath) {

    String schemaName;
    try {
      schemaName = dataPath.getSchema().getName();
    } catch (NoSchemaException e) {
      schemaName = null;
    }

    String catalogName;
    try {
      catalogName = dataPath.getCatalogDataPath().getName();
    } catch (NoCatalogException e) {
      catalogName = null;
    }
    SqlConnection dataStore = this.getConnection();

    // MySQL for instance
    if (this.sqlConnection.getMetadata().isSchemaSeenAsCatalog()) {
      catalogName = schemaName;
      schemaName = null;
    }
    try (
      // ImportedKey = the primary keys imported by a table
      ResultSet fkResultSet = dataStore.getCurrentJdbcConnection().getMetaData().getImportedKeys(catalogName, schemaName, dataPath.getName())
    ) {

      return SqlMetaForeignKey.getForeignKeyMetaFromDriverResultSet(fkResultSet);

    } catch (Exception e) {
      String s = Strings.createMultiLineFromStrings(
        "Error when building Foreign Key (ie imported keys) for the table " + dataPath,
        e.getMessage()).toString();

      if (dataStore.getTabular().isStrictExecution()) {
        throw new RuntimeException(s, e);
      } else {
        SqlLog.LOGGER_DB_JDBC.warning(s);
        return new ArrayList<>();
      }
    }
  }

  /**
   * @param sqlScript      - the data path with a query (a select sql file or sql query)
   * @param targetDataPath - the target definition (ie for the name, optional, if null, name is taken from the script)
   * @return the targetDataPath
   */
  public SqlDataPath createAsView(SqlScript sqlScript, SqlDataPath targetDataPath) {


    String selectStatement = sqlScript.getSelect();
    if (selectStatement == null) {
      throw new IllegalArgumentException("No select sql statement has been found in the data resource (" + sqlScript + ")");
    }

    // note view name is already optionally quoted
    // view name
    String viewName = getViewName(sqlScript, targetDataPath);
    selectStatement = getViewStatement(selectStatement);
    // statement
    String viewStatement = "create view " + viewName + " as " + selectStatement;
    this.execute(viewStatement);
    return sqlConnection.getDataPath(viewName);

  }

  /**
   * Return the view name.
   * Why? Some database such as SqlServer does not support the full qualified name but just a name
   * It's an easy overwrite point for this case
   *
   * @param sqlScript      - the source of the query (sql or text file)
   * @param targetDataPath - the target for the name
   * @return the name quoted (ie {@link #createQuotedName(String) createQuotedName} has already been applied)
   */
  protected String getViewName(SqlScript sqlScript, SqlDataPath targetDataPath) {

    if (targetDataPath != null) {
      return targetDataPath.toSqlStringPathWithNameValidation();
    }

    /**
     * We add a prefix because a sql name cannot start with a number
     */
    return createQuotedName("view_anonymous_" + Digest.createFromString(Digest.Algorithm.MD5, sqlScript.getSelect()).getHashHex());

  }

  /**
   * A function that can be overridden
   * (Used to delete the `order by` in Sql Server)
   *
   * @param viewStatement - the view statement
   * @return a view statement
   */
  protected String getViewStatement(String viewStatement) {
    return viewStatement;
  }


  public String deleteQuoteIdentifier(String s) {
    return Strings.createFromString(s).trim(this.getConnection().getMetadata().getIdentifierQuote()).toString();
  }

  /**
   * If the data path exists in the metadata store:
   * * Set the type of the data path {@link DataPath#getMediaType()}
   * * and return true
   * or
   * * return false
   *
   * <p>
   * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
   * We are not using the {@link SqlDataSystem#exists(DataPath)} function
   * because this function will return true for a query
   * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
   */
  public SqlMediaType getObjectMediaTypeOrDefault(String catalog, String schema, String objectName) {


    // Query for instance
    if (objectName == null) {
      throw new InternalException("Object name cannot be null");
    }


    String[] allTypes = null;
    try (ResultSet tableResultSet = this.sqlConnection.getCurrentJdbcConnection().getMetaData().getTables(catalog, schema, objectName, allTypes)) {
      boolean exists = tableResultSet.next(); // For TYPE_FORWARD_ONLY
      if (exists) {
        /**
         * We can create a SQL object without a table type (data structure)
         * All object are tables, schema or catalog, see {@link SqlDataPath#mediaType}
         * Getting and creating a data def (data structure), update it
         */
        String table_type = tableResultSet.getString("TABLE_TYPE");
        try {
          return SqlMediaType.castsToSqlType(table_type);
        } catch (NotSupportedException e) {
          // should not happen
          throw new InternalException("The table type (" + table_type + ") from the database is not a supported table type");
        }

      }

      /**
       * If none specified and non-existing
       * We think that the user want a table object
       */
      return TABLE;

    } catch (SQLException e) {
      throw new RuntimeException("We couldn't read the metadata for the object (" + objectName + "). Error: " + e.getMessage(), e);
    }

  }


  @Override
  public MediaType getContainerMediaType() {
    return SqlMediaType.SCHEMA;
  }


  @Override
  public SqlDataTypeVendor getSqlDataTypeVendor(KeyNormalizer typeName, int typeCode) {

    if (typeName == null && typeCode == 0) {
      throw new InternalException("typeName and typeCode can't be null together");
    }
    SqlTypeKeyUniqueIdentifier sqlKeyIdentifier = this.getSqlTypeKeyUniqueIdentifier();
    for (SqlDataTypeVendor sqlDataType : getSqlDataTypeVendors()) {
      if (sqlDataType.getVendorTypeNumber() != typeCode
        && sqlKeyIdentifier.equals(SqlTypeKeyUniqueIdentifier.NAME_AND_CODE)) {
        continue;
      }
      if (sqlDataType.toKeyNormalizer().equals(typeName)) {
        return sqlDataType;
      }
      for (KeyInterface name : sqlDataType.getAliases()) {
        if (name.toKeyNormalizer().equals(typeName)) {
          return sqlDataType;
        }
      }
    }

    /**
     * It may be a standard type
     * (ie Postgres has a `date`, we didn't add it at all in {@link
     */
    return super.getSqlDataTypeVendor(typeName, typeCode);

  }

}
