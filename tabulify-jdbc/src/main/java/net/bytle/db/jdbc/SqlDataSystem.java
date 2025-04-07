package net.bytle.db.jdbc;

import net.bytle.db.connection.Connection;
import net.bytle.db.exception.DataResourceNotEmptyException;
import net.bytle.db.model.*;
import net.bytle.db.spi.DataPath;
import net.bytle.db.spi.DataSystemAbs;
import net.bytle.db.spi.Tabulars;
import net.bytle.db.transfer.*;
import net.bytle.exception.*;
import net.bytle.regexp.Glob;
import net.bytle.type.Casts;
import net.bytle.type.MediaType;
import net.bytle.type.Strings;

import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

import static net.bytle.db.jdbc.SqlDataPathType.TABLE;
import static net.bytle.db.transfer.TransferOperation.COPY;


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
  public String createInsertStatementWithSelect(TransferSourceTarget transferSourceTarget) {

    transferSourceTarget.checkBeforeInsert();

    SqlDataPath target = (SqlDataPath) transferSourceTarget.getTargetDataPath();
    SqlDataPath source = (SqlDataPath) transferSourceTarget.getSourceDataPath();

    List<? extends ColumnDef> targetColumnsDefs = transferSourceTarget.getSourceColumnsInInsertStatement();

    return "insert into " +
      target.toSqlStringPath() +
      " ( " +
      createColumnsStatementForQuery(targetColumnsDefs) +
      " ) " +
      createOrGetQuery(source);

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
   * Return a insert statement again the tableDef from the resultSetMetdata
   *
   * @param sqlBindVariableFormat - do we create a sql parametrized statement (ie with ?) in place of a printf format (ie %s)
   * @return an insert statement
   */
  public String createInsertStatementUtilityStatementGenerator(TransferSourceTarget transferSourceTarget, Boolean sqlBindVariableFormat) {

    transferSourceTarget.checkBeforeInsert();
    return createInsertStatementUtilityValuesClauseBefore(transferSourceTarget) +
      createInsertStatementUtilityValuesClauseGenerator(transferSourceTarget, sqlBindVariableFormat) +
      createInsertStatementUtilityValuesClauseAfter();

  }

  /**
   * @param sqlBindVariableFormat -  if true, the sql `?` character is used, otherwise the printf pattern `%s` is used
   * @return return a parametrized series of values in a sql or printf format
   */
  protected String createInsertStatementUtilityValuesClauseGenerator(TransferSourceTarget transferSourceTarget, Boolean sqlBindVariableFormat) {

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

  public String createInsertStatementWithPrintfExpressions(TransferSourceTarget transferSourceTarget) {
    return createInsertStatementUtilityStatementGenerator(transferSourceTarget, false);
  }

  /**
   * Return a parameterized insert statement again the tableDef from the resultSetMetdata
   */
  public String createInsertStatementWithBindVariables(TransferSourceTarget transferSourceTarget) {

    return createInsertStatementUtilityStatementGenerator(transferSourceTarget, true);

  }

  /**
   * @return if the table exist in the underlying database (actually the letter case is important)
   * <p>
   */

  @Override
  public Boolean exists(DataPath dataPath) {

    SqlDataPath sqlDataPath = (SqlDataPath) dataPath;
    SqlDataPathType sqlType = sqlDataPath.getMediaType();

    String catalog;
    try {
      catalog = sqlDataPath.getCatalogDataPath().getName();
    } catch (NoCatalogException e) {
      catalog = null;
    }

    switch (sqlType) {

      case CATALOG:
        throw new RuntimeException("Catalog exists is not yet supported");
      case SCHEMA:

        final String schemaPattern = sqlDataPath.getName();

        List<SqlDataPath> schemas = this.getConnection().getSchemas(catalog, schemaPattern);
        switch (schemas.size()) {
          case 0:
            return false;
          case 1:
            return true;
          default:
            throw new RuntimeException("In exist, the number of schema returned should be 0 or 1. It was " + schemas.size());
        }

      default:
        try {

          String schema;
          try {
            schema = sqlDataPath.getSchema().getName();
          } catch (NoSchemaException e) {
            schema = null;
          }

          String[] allTypes = null; // null  means all types
          //noinspection ConstantConditions
          try (ResultSet tableResultSet = this.sqlConnection.getCurrentConnection().getMetaData().getTables(catalog, schema, sqlDataPath.getName(), allTypes)) {
            return tableResultSet.next(); // For TYPE_FORWARD_ONLY
          }

        } catch (SQLException e) {
          throw new RuntimeException(e);
        }
    }


  }


  @Override
  public Boolean isEmpty(DataPath dataPath) {

    return dataPath.getCount() == 0;

  }


  /**
   * When selecting a data path, you need to pass:
   * * the table name if a table
   * * the query if a query
   * This function returns this clause
   * You can then build a query such as:
   * * select count from fromClause
   * * select * from fromClause
   */
  protected String createFromClause(SqlDataPath sqlDataPath) {
    String fromClause = sqlDataPath.toSqlStringPath();
    if (sqlDataPath.getMediaType() == SqlDataPathType.SCRIPT) {
      /**
       * The alias is mandatory for postgres
       */
      fromClause = "(" + sqlDataPath.getQuery() + ") " + createQuotedName(sqlDataPath.getName());
    }
    return fromClause;
  }

  @Override
  public boolean isDocument(DataPath dataPath) {
    SqlDataPath sqlDataPath = (SqlDataPath) dataPath;
    return sqlDataPath.isDocument();
  }


  @Override
  public String getString(DataPath dataPath) {
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * This is the implementation of data transfer that happens on the same data store
   * <p>
   * * A create table from
   * * A insert into from
   * * rename (move)
   */
  @Override
  public TransferListener transfer(DataPath source, DataPath target, TransferProperties transferProperties) {

    assert source.getConnection().equals(target.getConnection()) : "The datastore of the source (" + source.getConnection() + ") is not the same than the datastore of the target (" + target.getConnection() + ")";

    SqlDataPath sqlSource = (SqlDataPath) source;
    SqlDataPath sqlTarget = (SqlDataPath) target;

    TransferSourceTarget transferSourceTarget = new TransferSourceTarget(source, target, transferProperties);

    TransferListenerAtomic transferListener = new TransferListenerAtomic(transferSourceTarget);
    transferListener.setType(TransferType.LOCAL);

    // Load operation is needed before the checks
    TransferOperation loadOperation = transferProperties.getOperation();
    if (loadOperation == null) {
      loadOperation = getDefaultTransferOperation();
      transferProperties.setOperation(loadOperation);
      SqlLog.LOGGER_DB_JDBC.info("The load operation was not set, taking the default (" + loadOperation + ")");
    }

    // Check the source
    transferSourceTarget.sourcePreChecks();
    // Special create as
    boolean useCreateAsTarget = this.canCreateAsTableCanBeUsed(transferSourceTarget);
    // Target check
    transferSourceTarget.targetPreOperationsAndCheck(transferListener, !useCreateAsTarget);


    // Load
    switch (loadOperation) {
      case INSERT:
      case COPY:

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


          // The target table should be without rows
          if (loadOperation == COPY) {
            long count = target.getCount();
            if (count != 0) {
              throw new DataResourceNotEmptyException("In a copy operation, the target table should be empty. This is not the case. The target table (" + target + ") has (" + count + ") rows");
            }
          }

        }

        try (Statement statement = sqlSource.getConnection().getCurrentConnection().createStatement()) {
          boolean resultSetReturned = statement.execute(copyStatement);
          SqlLog.LOGGER_DB_JDBC.info("Data Transfer statement executed: " + Strings.createFromString(copyStatement).onOneLine().toString());
          if (!resultSetReturned) {
            int updateCount = statement.getUpdateCount();
            transferListener.incrementRows(updateCount);
            SqlLog.LOGGER_DB_JDBC.info(updateCount + " records were moved from (" + source + ") to (" + target + ")");
          }
        } catch (SQLException e) {
          final String msg = "Error when executing the statement: " + copyStatement;
          SqlLog.LOGGER_DB_JDBC.severe(msg);
          transferListener.addException(e);
          throw new RuntimeException(msg, e);
        }
        break;
      case MOVE:
        String alterTableName = createAlterTableRenameStatement(sqlSource, sqlTarget);
        transferListener.setMethod(TransferMethod.RENAME);
        SqlLog.LOGGER_DB_JDBC.info("Data Transfer using the (" + transferListener.getMethod() + ") method");
        try (Statement statement = sqlSource.getConnection().getCurrentConnection().createStatement()) {
          statement.execute(alterTableName);
          SqlLog.LOGGER_DB_JDBC.info("Data Transfer statement executed: " + Strings.createFromString(alterTableName).onOneLine().toString());
        } catch (SQLException e) {
          final String msg = "Error when executing the statement: " + alterTableName;
          SqlLog.LOGGER_DB_JDBC.severe(msg);
          transferListener.addException(e);
          throw new RuntimeException(msg, e);
        }
        break;
      case UPDATE:
        String updateStatement = createUpdateStatementWithSelect(transferSourceTarget);
        transferListener.setMethod(TransferMethod.UPDATE_FROM_QUERY);
        SqlLog.LOGGER_DB_JDBC.info("Data Transfer using the (" + transferListener.getMethod() + ") method.");
        try (Statement statement = sqlSource.getConnection().getCurrentConnection().createStatement()) {
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
        String upsertStatement = createUpsertStatementWithSelect(transferSourceTarget);
        transferListener.setMethod(TransferMethod.UPSERT_FROM_QUERY);
        SqlLog.LOGGER_DB_JDBC.info("Data Transfer using the (" + transferListener.getMethod() + ") method");
        try (Statement statement = sqlSource.getConnection().getCurrentConnection().createStatement()) {
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
        String deleteStatement = createDeleteStatementWithSelect(transferSourceTarget);
        transferListener.setMethod(TransferMethod.DELETE_FROM_QUERY);
        SqlLog.LOGGER_DB_JDBC.info("Data Transfer using the (" + transferListener.getMethod() + ") method.");
        try (Statement statement = sqlSource.getConnection().getCurrentConnection().createStatement()) {
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


  /**
   *
   * @param transferSourceTarget the transfer meta
   * @return if the create table as method can be used
   */
  private boolean canCreateAsTableCanBeUsed(TransferSourceTarget transferSourceTarget) {
    DataPath target = transferSourceTarget.getTargetDataPath();
    Set<TransferResourceOperations> targetOperations = transferSourceTarget.getTransferProperties().getTargetOperations();
    return (!exists(target) &&
      (
        target.getRelationDef() == null ||
          target.getRelationDef().getColumnsSize() == 0
      )
    ) || targetOperations.contains(TransferResourceOperations.REPLACE);
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
  protected String createTableAsStatement(TransferSourceTarget transferSourceTarget) {

    SqlDataPath target = (SqlDataPath) transferSourceTarget.getTargetDataPath();
    SqlDataPath source = (SqlDataPath) transferSourceTarget.getSourceDataPath();
    return "create table " +
      target.toSqlStringPath() +
      " as " +
      createOrGetQuery(source);
  }

  /**
   * Return the query if the data path is a query or create the select statement
   */
  protected String createOrGetQuery(SqlDataPath source) {
    if (source.getMediaType() == SqlDataPathType.SCRIPT) {
      return source.getQuery();
    } else {
      return createSelectStatement(source);
    }
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

    SqlDataPathType type = sqlResourcePath.getSqlMediaType();
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
         * In Tabli, null means the default
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
    try (
      ResultSet tableResultSet = sqlPrimaryKeyDataPath.getConnection().getCurrentConnection().getMetaData().getExportedKeys(catalog, schema, tableName)
    ) {
      fkDatas = SqlMetaForeignKey.getForeignKeyMetaFromDriverResultSet(tableResultSet);
    } catch (SQLException e) {
      String s = "Error when getting the foreign keys that references " + primaryKeyDataPath + " (ie getExportedKeys function) ";
      if (primaryKeyDataPath.getConnection().getTabular().isStrict()) {
        throw new RuntimeException(s, e);
      } else {
        SqlLog.LOGGER_DB_JDBC.warning(s + "\n" + e.getMessage());
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
      foreignKeyDefs.add(
        ForeignKeyDef.createOf(
            foreignTable.getOrCreateRelationDef(),
            primaryKey,
            sqlMeta.getForeignKeyColumns()).
          setName(sqlMeta.getName())
      );

    }
    return foreignKeyDefs;

  }


  /**
   * Create a drop statement for a {@link Constraint}
   */
  protected String dropConstraintStatement(Constraint constraint) {
    StringBuilder dropConstraintStatement = new StringBuilder();
    dropConstraintStatement.append("alter ");
    SqlDataPath table = (SqlDataPath) constraint.getRelationDef().getDataPath();
    SqlDataPathType type = table.getMediaType();
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
    try (Statement sqlStatement = table.getConnection().getCurrentConnection().createStatement()) {

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
  public void create(DataPath dataPath) {


    SqlDataPath sqlDataPath = (SqlDataPath) dataPath;

    SqlDataPathType enumObjectType = sqlDataPath.getMediaType();
    if (enumObjectType == SqlDataPathType.UNKNOWN) {
      enumObjectType = TABLE;
    }

    if (enumObjectType == TABLE) {

      // Check that the foreign tables exist
      for (ForeignKeyDef foreignKeyDef : dataPath.getOrCreateRelationDef().getForeignKeys()) {
        DataPath foreignDataPath = foreignKeyDef.getForeignPrimaryKey().getRelationDef().getDataPath();
        if (!exists(foreignDataPath)) {
          throw new RuntimeException("The foreign table (" + foreignDataPath.toString() + ") does not exist");
        }
      }

      // Create the table
      this.execute(createTableStatement(sqlDataPath));

      /**
       * Add the constraints
       * <p>
       * We are not throwing an error when a constraint statement is asked
       * because the CREATE statement is seen as a unit.
       * There is not a lot of change to get an constraint statement (alter)
       * without the create one
       * The constraint statement function are splits
       * to be able to test them
       *
       */
      // Add the Primary Key
      final PrimaryKeyDef primaryKey = dataPath.getOrCreateRelationDef().getPrimaryKey();
      if (primaryKey != null) {
        if (!primaryKey.getColumns().isEmpty()) {
          String createPrimaryKeyStatement = createPrimaryKeyStatement(sqlDataPath);
          if (createPrimaryKeyStatement != null) {
            this.execute(createPrimaryKeyStatement);
          }
        }
      }

      // Foreign key
      for (ForeignKeyDef foreignKeyDef : dataPath.getOrCreateRelationDef().getForeignKeys()) {
        String createForeignKeyStatement = createForeignKeyStatement(foreignKeyDef);
        if (createForeignKeyStatement != null) {
          this.execute(createForeignKeyStatement);
        }
      }

      // Unique key
      for (UniqueKeyDef uniqueKeyDef : dataPath.getOrCreateRelationDef().getUniqueKeys()) {
        String createUniqueKeyStatement = createUniqueKeyStatement(uniqueKeyDef);
        if (createUniqueKeyStatement != null) {
          this.execute(createUniqueKeyStatement);
        }
      }

      SqlLog.LOGGER_DB_JDBC.info("Table (" + dataPath + ") created");

    } else if (enumObjectType == SqlDataPathType.SCRIPT) {

      this.execute(createViewStatement(sqlDataPath));
      SqlLog.LOGGER_DB_JDBC.info("View (" + dataPath + ") created from query");

    } else {

      throw new UnsupportedOperationException("The data resources (" + dataPath + ") is a " + enumObjectType + " and the creation of the kind of SQL data resource is not yet supported.");

    }

  }


  /**
   * @return a create statement without pk and fk
   * For a primary key, see {@link #createPrimaryKeyStatement(SqlDataPath)}
   * For a foreign key, see {@link #createForeignKeyStatement(ForeignKeyDef)}
   */
  public String createTableStatement(SqlDataPath dataPath) {

    return "create table " +
      dataPath.toSqlStringPath() +
      " (\n" +
      createColumnsStatement(dataPath) +
      " )\n";

  }

  /**
   * @param dataPath : The target schema
   * @return the column string part of a create statement
   */
  protected String createColumnsStatement(DataPath dataPath) {


    StringBuilder statementColumnPart = new StringBuilder();
    RelationDef dataDef = dataPath.getOrCreateRelationDef();
    for (int i = 1; i <= dataDef.getColumnsSize(); i++) {

      try {

        ColumnDef columnDef = dataDef.getColumnDef(i);
        // Add it to the columns statement
        statementColumnPart.append(createColumnStatement(columnDef));

      } catch (Exception e) {

        throw new RuntimeException(e + "\nException: The Column Statement build until now is:\n" + statementColumnPart, e);

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
   * @return The statement is the create data type statement that should be compliant
   * with the actual connection.
   */
  protected String createDataTypeStatement(ColumnDef columnDef) {

    /**
     * Processing var
     */
    SqlDataType sqlDataType = columnDef.getDataType();
    Connection columnSourceConnection = columnDef.getRelationDef().getDataPath().getConnection();

    /**
     * Type translation.
     * The table can come from another connection
     * For instance, from a yaml file
     */
    SqlDataType targetSqlType = sqlConnection.getSqlDataTypeFromSourceDataType(sqlDataType);

    /**
     * Precision verification
     */
    Integer precision = columnDef.getPrecision();
    Integer maxPrecision = targetSqlType.getMaxPrecision();
    Integer defaultPrecision = targetSqlType.getDefaultPrecision();
    if (precision != null && maxPrecision != null && precision > maxPrecision) {
      String message = "The precision (" + precision + ") of the column (" + columnDef + ") is greater than the maximum allowed (" + maxPrecision + ") for the datastore (" + columnSourceConnection.getName() + ")";
      SqlLog.LOGGER_DB_JDBC.warning(message);
    }

    /**
     * Scale verification
     */
    Integer scale = columnDef.getScale();
    Integer maximumScale = targetSqlType.getMaximumScale();
    if (scale != null && maximumScale != null && scale > maximumScale) {
      String message = "The scale (" + scale + ") of the column (" + columnDef + ") is greater than the maximum allowed (" + maximumScale + ") for the datastore (" + columnSourceConnection.getName() + ")";
      SqlLog.LOGGER_DB_JDBC.warning(message);
    }


    /**
     * Create the data type statement
     */
    String dataTypeCreateStatement = targetSqlType.getSqlName();
    int targetTypeCode = targetSqlType.getTypeCode();
    switch (targetTypeCode) {
      case Types.BIT:
      case Types.BIGINT:
      case Types.INTEGER:
      case Types.SMALLINT:
      case Types.REAL:
      case Types.DOUBLE:
      case Types.FLOAT:
      case Types.DATE:
      case Types.BOOLEAN:
      case SqlTypes.JSON:
      case Types.SQLXML:
      case Types.CLOB:
      case Types.BLOB:
      case Types.LONGVARCHAR: // clob, text mapping
        // DataType without precision (Ie they are in the name)
        return dataTypeCreateStatement;
      case Types.TIMESTAMP_WITH_TIMEZONE:
        if (!(precision == null || precision.equals(defaultPrecision))) {
          return "timestamp(" + precision + ") with time zone";
        } else {
          return dataTypeCreateStatement;
        }
      case Types.TIME_WITH_TIMEZONE:
        if (!(precision == null || precision.equals(defaultPrecision))) {
          return "time(" + precision + ") with time zone";
        } else {
          return dataTypeCreateStatement;
        }
      case Types.TIMESTAMP:
        // timestamp without timezone
        // timestamp precision if not specified is generally implicitly 6 (ie precision is optional)
        // https://www.postgresql.org/docs/current/datatype-datetime.html
        if (!(precision == null || precision.equals(defaultPrecision))) {
          return "timestamp(" + precision + ")";
        } else {
          return dataTypeCreateStatement;
        }
      case Types.TIME:
      case Types.VARCHAR:
      case Types.NVARCHAR:
      case Types.CHAR:
      case Types.NCHAR:
        /**
         * This data type have one precision
         * and should all declare their default precision if not set
         */
        if (defaultPrecision == null) {
          if(targetTypeCode == Types.TIME){
            // implicit zero
            // https://datacadamia.com/data/type/relation/sql/time
            defaultPrecision = 0;
          } else {
            throw new RuntimeException("The default precision is null for the data type (" + targetSqlType + ") and this is not allowed for a data type with precision");
          }
        }
        if (precision == null) {
          precision = defaultPrecision;
        }
        if (precision.equals(defaultPrecision)) {
          if (targetSqlType.getMandatoryPrecision() != null && targetSqlType.getMandatoryPrecision()) {
            return dataTypeCreateStatement + "(" + precision + ")";
          } else {
            return dataTypeCreateStatement;
          }
        } else {
          return dataTypeCreateStatement + "(" + precision + ")";
        }
      case Types.DECIMAL:
      case Types.NUMERIC:
        if ((precision == null || precision.equals(defaultPrecision)) && (scale == null || scale == 0 || scale.equals(maximumScale))) {
          return dataTypeCreateStatement;
        } else {
          if (precision == null) {
            precision = targetSqlType.getMaxPrecision();
          }
          return dataTypeCreateStatement + "(" + precision + (scale != null && scale != 0 ? "," + scale : "") + ")";
        }


      default:
        if (precision != null && precision > 0) {
          return dataTypeCreateStatement + "(" + precision + (scale != null && scale != 0 ? "," + scale : "") + ")";
        } else {
          return dataTypeCreateStatement;
        }
    }


  }

  /**
   * @param uniqueKeyDef - The source unique key def
   * @return an alter table unique statement
   */
  protected String createUniqueKeyStatement(UniqueKeyDef uniqueKeyDef) {

    String statement = "ALTER TABLE " + ((SqlDataPath) uniqueKeyDef.getRelationDef().getDataPath()).toSqlStringPath() + " ADD ";

// The series of columns definitions (col1, col2,...)
    final List<ColumnDef> columns = uniqueKeyDef.getColumns();
    List<String> columnNames = new ArrayList<>();
    for (ColumnDef columnDef : columns) {
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
  protected String createColumnStatement(ColumnDef columnDef) {


    String dataTypeCreateStatement;

    dataTypeCreateStatement = createDataTypeStatement(columnDef);


    // NOT NULL / Optionality
    String notNullStatement = "";
    PrimaryKeyDef primaryKey = columnDef.getRelationDef().getPrimaryKey();
    List<ColumnDef> primaryKeyColumns = new ArrayList<>();
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
  public String createUpsertStatementWithSelect(TransferSourceTarget transferSourceTarget) {


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
  protected String createUpsertStatementUtilityOnConflict(TransferSourceTarget transferSourceTarget) {


    /**
     * Build the targetUniqueKeyFoundInSourceColumns
     * with the target unique constraint columns found in the source
     */
    List<ColumnDef> uniqueColumnsForTarget = transferSourceTarget.getSourceUniqueColumnsForTarget();
    List<String> uniqueColumnsNameForTarget = uniqueColumnsForTarget.stream().map(ColumnDef::getColumnName).collect(Collectors.toList());
    SqlDataPath targetDataPath = (SqlDataPath) transferSourceTarget.getTargetDataPath();
    List<UniqueKeyDef> targetUniqueKeyFoundInSourceColumns = new ArrayList<>();
    for (UniqueKeyDef targetUniqueKey : targetDataPath.getOrCreateRelationDef().getUniqueKeys()) {
      boolean notColumnFound = false;
      for (ColumnDef column : targetUniqueKey.getColumns()) {
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

    /**
     * Build the statement
     */
    StringBuilder upsertStatement = new StringBuilder();
    if (!targetUniqueKeyFoundInSourceColumns.isEmpty()) {
      UniqueKeyDef targetUniqueKey = targetUniqueKeyFoundInSourceColumns.get(0);
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
   * @param foreignKeyDef - The source foreign key
   * @return a alter table foreign key statement
   */
  protected String createForeignKeyStatement(ForeignKeyDef foreignKeyDef) {

    SqlConnection jdbcDataSystem = (SqlConnection) foreignKeyDef.getRelationDef().getDataPath().getConnection();

    // Constraint are supported from 2.1
    // https://issues.apache.org/jira/browse/HIVE-13290
    if (jdbcDataSystem.getProductName().equals(SqlConnection.DB_HIVE)) {
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
    final List<ColumnDef> nativeColumns = foreignKeyDef.getChildColumns();
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
    List<ColumnDef> foreignColumns = foreignDataPath.getOrCreateRelationDef().getPrimaryKey().getColumns();
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
    List<ColumnDef> columns = primaryKey.getColumns();
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
    for (ColumnDef columnDef : columns) {
      columnNames.add(this.createQuotedName(columnDef.getColumnName()));
    }
    statement
      .append("PRIMARY KEY  (")
      .append(String.join(", ", columnNames))
      .append(")");

    return statement.toString();
  }


  @Override
  public void drop(DataPath dataPath) {

    SqlDataPath sqlDataPath = (SqlDataPath) dataPath;
    SqlDataPathType type = sqlDataPath.getMediaType();
    switch (type) {
      case TABLE:
      case VIEW:
        // supported
        break;
      case SYSTEM_TABLE:
      case SYSTEM_VIEW:
        // Not supported, but we don't return an error because
        // a `*@sqlite` selection returns them by default for now
        SqlLog.LOGGER_DB_JDBC.warning("The resource ("+sqlDataPath+") is not a (" + type + ") and was not dropped");
        return;
      default:
        throw new UnsupportedOperationException("The resource ("+sqlDataPath+") is not a view or a table. It's a (" + type + "). We don't support a drop");
    }


    String dropTableStatement = createDropTableStatement(sqlDataPath);

    try (Statement statement = sqlDataPath.getConnection().getCurrentConnection().createStatement()) {

      SqlLog.LOGGER_DB_JDBC.fine("Trying to drop " + type + " " + dataPath);
      statement.execute(dropTableStatement);
      String typeCamelCased = Strings.createFromString(type.toString()).toFirstLetterCapitalCase().toString();
      SqlLog.LOGGER_DB_JDBC.info(typeCamelCased + " (" + dataPath + ") dropped.");

    } catch (SQLException e) {
      String msg = Strings.createMultiLineFromStrings("Dropping of the data path (" + sqlDataPath + ") was not successful with the statement `" + dropTableStatement + "`"
        , "Cause: " + e.getMessage()).toString();
      SqlLog.LOGGER_DB_JDBC.severe(msg);
      throw new RuntimeException(msg, e);
    }

  }

  protected String createDropTableStatement(SqlDataPath sqlDataPath) {
    StringBuilder dropTableStatement = new StringBuilder();
    dropTableStatement.append("drop ");
    SqlDataPathType enumObjectType = sqlDataPath.getMediaType();
    switch (enumObjectType) {
      case TABLE:
        dropTableStatement.append("table ");
        break;
      case VIEW:
        dropTableStatement.append("view ");
        break;
      default:
        throw new RuntimeException("The drop of the SQL object type (" + enumObjectType + ") is not implemented");
    }
    dropTableStatement.append(sqlDataPath.toSqlStringPath());
    return dropTableStatement.toString();
  }

  @Override
  public void delete(DataPath dataPath) {

    SqlDataPath sqlDataPath = (SqlDataPath) dataPath;
    //noinspection SqlDialectInspection,SqlWithoutWhere
    String deleteStatement = "delete from " + sqlDataPath.toSqlStringPath();
    try (Statement statement = sqlDataPath.getConnection().getCurrentConnection().createStatement()) {
      statement.execute(deleteStatement);
      // Without commit, the database is locked for sqlite (if the connection is no more in autocommit mode)
      sqlDataPath.getConnection().getCurrentConnection().commit();
      SqlLog.LOGGER_DB_JDBC.info("Table " + dataPath.getConnection() + " deleted");
    } catch (SQLException e) {
      throw new RuntimeException("The delete statement has a problem: " + deleteStatement, e);
    }

  }

  @Override
  public void truncate(List<DataPath> dataPaths) {

    List<SqlDataPath> jdbcDataPaths = Casts.castToListSafe(dataPaths, SqlDataPath.class);

    /**
     * Check if the truncated table
     * have no dependencies or that the dependencies
     * are also in the tables to truncate
     * Normally, the database do this constraint
     * but as Sqlite does not, we do
     */
    for (SqlDataPath sqlDataPath : jdbcDataPaths) {
      for (ForeignKeyDef fkDependency : Tabulars.getReferences(sqlDataPath)) {
        SqlDataPath dependentTable = (SqlDataPath) fkDependency.getRelationDef().getDataPath();
        if (!jdbcDataPaths.contains(dependentTable)) {
          throw new RuntimeException("The table (" + sqlDataPath + ") cannot be truncated because the table (" + dependentTable + ") dependent on it and is not in the tables to truncate. Add the table (" + fkDependency + ") into the tables to truncate or delete the foreign key (" + fkDependency + ").");
        }
      }
    }

    /**
     * Truncating
     */
    List<String> sqls = createTruncateStatement(jdbcDataPaths);
    for (String sql : sqls) {
      try (Statement statement = jdbcDataPaths.get(0).getConnection().getCurrentConnection().createStatement()) {
        statement.execute(sql);
        SqlLog.LOGGER_DB_JDBC.info("Truncate Statement executed: " + Strings.createFromString(sql).onOneLine().toString());
      } catch (SQLException e) {
        throw new RuntimeException("Bad Sql:" + Strings.createFromString(sql).onOneLine().toString(), e);
      }
    }
    SqlLog.LOGGER_DB_JDBC.info("Table(s) (" + dataPaths.stream().map(DataPath::toString).collect(Collectors.joining(", ")) + ") were truncated");

  }

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
    SqlDataPathType type = sqlDataPath.getMediaType();
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
  public void execute(DataPath dataPath) {
    execute(dataPath.getScript());
  }

  /**
   * Execute a sql statement
   */
  public void execute(String statement) {
    try (Statement sqlStatement = this.getConnection().getCurrentConnection().createStatement()) {
      sqlStatement.execute(statement);
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
       * try to create view to determine the columns
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

    String identifierQuoteString = sqlConnection.getMetadata().getIdentifierQuote();
    return identifierQuoteString + word + identifierQuoteString;


  }

  /**
   * Build the metadata type
   */
  public Map<Integer, SqlMetaDataType> getMetaDataTypes() {

    try {
      ResultSet typeInfoResultSet = sqlConnection.getCurrentConnection().getMetaData().getTypeInfo();
      Map<Integer, SqlMetaDataType> sqlMetaDataTypes = new HashMap<>();
      while (typeInfoResultSet.next()) {
        int typeCode = typeInfoResultSet.getInt("DATA_TYPE");
        SqlMetaDataType sqlDataType = new SqlMetaDataType(typeCode);
        sqlMetaDataTypes.put(typeCode, sqlDataType);
        String typeName = typeInfoResultSet.getString("TYPE_NAME");
        sqlDataType.setSqlName(typeName);
        int precision = typeInfoResultSet.getInt("PRECISION");
        sqlDataType.setMaxPrecision(precision);
        String literalPrefix = typeInfoResultSet.getString("LITERAL_PREFIX");
        sqlDataType.setLiteralPrefix(literalPrefix);
        String literalSuffix = typeInfoResultSet.getString("LITERAL_SUFFIX");
        sqlDataType.setLiteralSuffix(literalSuffix);
        String createParams = typeInfoResultSet.getString("CREATE_PARAMS");
        sqlDataType.setCreateParams(createParams);
        Short nullable = typeInfoResultSet.getShort("NULLABLE");
        sqlDataType.setNullable(nullable);
        Boolean caseSensitive = typeInfoResultSet.getBoolean("CASE_SENSITIVE");
        sqlDataType.setCaseSensitive(caseSensitive);
        Short searchable = typeInfoResultSet.getShort("SEARCHABLE");
        sqlDataType.setSearchable(searchable);
        Boolean unsignedAttribute = typeInfoResultSet.getBoolean("UNSIGNED_ATTRIBUTE");
        sqlDataType.setUnsignedAttribute(unsignedAttribute);
        Boolean fixedPrecScale = typeInfoResultSet.getBoolean("FIXED_PREC_SCALE");
        sqlDataType.setFixedPrecisionScale(fixedPrecScale);
        Boolean autoIncrement = typeInfoResultSet.getBoolean("AUTO_INCREMENT");
        sqlDataType.setAutoIncrement(autoIncrement);
        String localTypeName = typeInfoResultSet.getString("LOCAL_TYPE_NAME");
        sqlDataType.setLocalTypeName(localTypeName);
        Integer minimumScale = (int) typeInfoResultSet.getShort("MINIMUM_SCALE");
        sqlDataType.setMinimumScale(minimumScale);
        Integer maximumScale = (int) typeInfoResultSet.getShort("MAXIMUM_SCALE");
        sqlDataType.setMaximumScale(maximumScale);
      }
      return sqlMetaDataTypes;
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }

  }

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

    try (
      ResultSet columnResultSet = dataPath.getConnection().getCurrentConnection().getMetaData().getColumns(catalog, schemaName, dataPath.getName(), null)
    ) {
      while (columnResultSet.next()) {

        SqlMetaColumn meta = SqlMetaColumn.createOf(dataPath, columnResultSet.getString("COLUMN_NAME"));
        sqlMetaColumns.add(meta);

        // Not implemented on all driver (example: sqliteDriver)
        try {
          meta.setIsGeneratedColumn(columnResultSet.getString("IS_GENERATEDCOLUMN"));
        } catch (SQLException e) {
          SqlLog.LOGGER_DB_JDBC.fine("The IS_GENERATEDCOLUMN column seems not to be implemented. Message: " + e.getMessage());
        }

        // Not implemented on all driver (example: sqliteDriver)
        try {
          meta.setIsAutoIncrement(columnResultSet.getString("IS_AUTOINCREMENT"));
        } catch (SQLException e) {
          SqlLog.LOGGER_DB_JDBC.fine("The IS_AUTOINCREMENT column seems not to be implemented. Message: " + e.getMessage());
        }

        int data_type = columnResultSet.getInt("DATA_TYPE");
        Integer scale = columnResultSet.getInt("DECIMAL_DIGITS");
        int precision = columnResultSet.getInt("COLUMN_SIZE");

        /**
         * For Postgresql, Sqlserver, by default
         * the precision of the time is in the scale
         */
        if (SqlTypes.timeTypes.contains(data_type)) {
          precision = scale;
          scale = null;
        }

        meta
          .setPrecision(precision)
          .setTypeCode(data_type)
          .setTypeName(columnResultSet.getString("TYPE_NAME"))
          .setScale(scale)
          .setIsNullable(columnResultSet.getInt("NULLABLE"));
      }

    } catch (
      SQLException e) {
      throw new RuntimeException(e);
    }
    return sqlMetaColumns;
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
  public String createUpdateStatementWithSelect(TransferSourceTarget transferSourceTarget) {

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
    if (source.getMediaType() == SqlDataPathType.SCRIPT) {
      update.append("( ")
        .append(source.getQuery())
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
  private String createUpdateStatementUtilityFirstPartUntilSet(TransferSourceTarget transferSourceTarget) {
    /**
     * Build the update statement
     */
    return "update " +
      ((SqlDataPath) transferSourceTarget.getTargetDataPath()).toSqlStringPath() +
      " as " +
      transferSourceTarget.getTargetAlias() +
      " set";
  }

  /**
   * Create a view statement from a query data path
   */
  public String createViewStatement(SqlDataPath dataPath) {
    if (dataPath.getMediaType() == SqlDataPathType.SCRIPT) {
      String query = createOrGetQuery(dataPath);
      return "create view " + dataPath.toSqlStringPath() + " as " + query;
    } else {
      /**
       * We need a name for the view
       * A view is just a stored query
       */
      throw new UnsupportedOperationException("A create view statement is only support for a query data resource");
    }
  }

  @Override
  public void dropNotNullConstraint(DataPath dataPath) {
    for (ColumnDef columnDef : dataPath.getOrCreateRelationDef().getColumnDefs()) {
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
  protected String createDropNotNullConstraintStatement(ColumnDef columnDef) {
    SqlDataPath sqlDataPath = (SqlDataPath) columnDef.getRelationDef().getDataPath();
    return "alter table " + sqlDataPath.toSqlStringPath() + " alter column " + createQuotedName(columnDef.getColumnName()) + " drop not null";
  }

  public TransferOperation getDefaultTransferOperation() {
    return TransferOperation.INSERT;
  }

  /**
   * Create upsert from values statement
   */
  public String createUpsertStatementWithPrintfExpressions(TransferSourceTarget transferSourceTarget) {
    return createUpsertStatementUtilityValuesPartBefore(transferSourceTarget) +
      createInsertStatementUtilityValuesClauseGenerator(transferSourceTarget, false) +
      createUpsertStatementUtilityValuesPartAfter(transferSourceTarget);
  }

  public String createUpsertStatementWithBindVariables(TransferSourceTarget transferSourceTarget) {
    return createUpsertStatementUtilityValuesPartBefore(transferSourceTarget) +
      createInsertStatementUtilityValuesClauseGenerator(transferSourceTarget, true) +
      createUpsertStatementUtilityValuesPartAfter(transferSourceTarget);
  }

  /**
   * The insert statement before the values
   * The insert statement is split in two to not build it again on every insert
   */
  public String createInsertStatementUtilityValuesClauseBefore(TransferSourceTarget transferSourceTarget) {

    transferSourceTarget.checkBeforeInsert();

    RelationDef target = transferSourceTarget.getTargetDataPath().getOrCreateRelationDef();
    final SqlDataPath dataPath = (SqlDataPath) target.getDataPath();
    StringBuilder insertStatement = new StringBuilder();
    insertStatement.append("insert into ")
      .append(dataPath.toSqlStringPath())
      .append(" ( ");

    List<? extends ColumnDef> targetColumnsToLoad = transferSourceTarget.getSourceColumnsInInsertStatement();
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
  public String createUpsertStatementUtilityValuesPartBefore(TransferSourceTarget transferSourceTarget) {
    return createInsertStatementUtilityValuesClauseBefore(transferSourceTarget);
  }

  /**
   * @return the last part of a upsert with values statement just after that the values are given
   * <p>
   * Because a upsert statement with values should be rewritten each time, the sql statement part
   * that are before and after the values are computed only once
   */
  public String createUpsertStatementUtilityValuesPartAfter(TransferSourceTarget transferSourceTarget) {
    return createInsertStatementUtilityValuesClauseAfter() + " " +
      createUpsertStatementUtilityOnConflict(transferSourceTarget);
  }

  /**
   * @return a update statement with Sql bind variable if sqlBindVariable is true, otherwise with a printf expression
   */
  protected String createUpdateStatementUtilityStatementGenerator(TransferSourceTarget transferSourceTarget, Boolean sqlBindVariable) {
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

  public String createUpdateStatementWithBindVariables(TransferSourceTarget transferSourceTarget) {

    return createUpdateStatementUtilityStatementGenerator(transferSourceTarget, true);
  }


  public String createUpdateStatementWithPrintfExpressions(TransferSourceTarget transferSourceTarget) {
    return createUpdateStatementUtilityStatementGenerator(transferSourceTarget, false);
  }

  /**
   * <a href="https://www.postgresql.org/docs/10/sql-delete.html">...</a>
   */
  public String createDeleteStatementWithSelect(TransferSourceTarget transferSourceTarget) {
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
      .append(" ) in ( select ")
      .append(uniqueColumnsClause)
      .append(" from ");

    SqlDataPath sourceDataPath = (SqlDataPath) transferSourceTarget.getSourceDataPath();
    SqlDataPathType enumObjectType = sourceDataPath.getMediaType();
    switch (enumObjectType) {
      case TABLE:
        delete
          .append(sourceDataPath.toSqlStringPath())
          .append(" )");
        break;
      case SCRIPT:
        delete
          .append("( ")
          .append(sourceDataPath.getQuery())
          .append(" ) ")
          .append(createQuotedName(sourceDataPath.getLogicalName()))
          .append(" )");
        break;
      default:
        throw new UnsupportedOperationException("The creation of a SQL delete statement is not supported for a data path with the type (" + enumObjectType + ")");
    }
    return delete.toString();
  }

  public String createDeleteStatementWithPrintfExpressions(TransferSourceTarget transferSourceTarget) {
    return createDeleteStatementUtilityStatementGenerator(transferSourceTarget, false);
  }

  public String createDeleteStatementWithBindVariables(TransferSourceTarget transferSourceTarget) {
    return createDeleteStatementUtilityStatementGenerator(transferSourceTarget, true);
  }

  private String createDeleteStatementUtilityStatementGenerator(TransferSourceTarget transferSourceTarget, boolean sqlBindVariable) {
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
    try (
      // ImportedKey = the primary keys imported by a table
      ResultSet fkResultSet = dataStore.getCurrentConnection().getMetaData().getImportedKeys(catalogName, schemaName, dataPath.getName())
    ) {

      return SqlMetaForeignKey.getForeignKeyMetaFromDriverResultSet(fkResultSet);

    } catch (Exception e) {
      String s = Strings.createMultiLineFromStrings(
        "Error when building Foreign Key (ie imported keys) for the table " + dataPath,
        e.getMessage()).toString();

      if (dataStore.getTabular().isStrict()) {
        throw new RuntimeException(s, e);
      } else {
        SqlLog.LOGGER_DB_JDBC.warning(s);
        return new ArrayList<>();
      }
    }
  }

  public SqlDataPath createViewFromQueryDataPath(SqlDataPath queryDataPath) {
    String viewStatement = sqlConnection.getDataSystem().createViewStatement(queryDataPath);
    this.execute(viewStatement);
    return sqlConnection.getDataPath(queryDataPath.toSqlStringPath());
  }

  public String deleteQuoteIdentifier(String s) {
    return Strings.createFromString(s).trim(this.getConnection().getMetadata().getIdentifierQuote()).toString();
  }

  /**
   * If the data path exists in the meta data store:
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
  public SqlDataPathType getObjectMediaTypeOrDefault(String catalog, String schema, String objectName) {


    try {

      // Query for instance
      if (objectName == null) {
        throw new InternalException("Object name cannot be null");
      }


      String[] allTypes = null;
      try (ResultSet tableResultSet = this.sqlConnection.getCurrentConnection().getMetaData().getTables(catalog, schema, objectName, allTypes)) {
        boolean exists = tableResultSet.next(); // For TYPE_FORWARD_ONLY
        if (exists) {
          /**
           * We can create a SQL object without a table type (data structure)
           * All object are tables, schema or catalog, see {@link SqlDataPath#mediaType}
           * Getting and creating a data def (data structure), update it
           */
          String table_type = tableResultSet.getString("TABLE_TYPE");
          try {
            return SqlDataPathType.getSqlType(table_type);
          } catch (NotSupportedException e) {
            // should not happen
            throw new InternalException("The table type from the database (" + table_type + ") is not a supported table type");
          }

        } else {

          return TABLE;

        }
      }


    } catch (SQLException e) {
      throw new RuntimeException(e);
    }

  }

}
