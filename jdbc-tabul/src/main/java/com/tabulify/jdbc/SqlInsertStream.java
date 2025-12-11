package com.tabulify.jdbc;

import com.tabulify.model.ColumnDef;
import com.tabulify.model.RelationDef;
import com.tabulify.model.SqlDataType;
import com.tabulify.spi.DataPath;
import com.tabulify.spi.Tabulars;
import com.tabulify.stream.InsertStream;
import com.tabulify.stream.InsertStreamAbs;
import com.tabulify.transfer.*;
import com.tabulify.exception.MissingSwitchBranch;
import com.tabulify.exception.NoColumnException;
import com.tabulify.type.Casts;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static com.tabulify.transfer.UpsertType.*;

public class SqlInsertStream extends InsertStreamAbs implements InsertStream, AutoCloseable {

  public static final Logger LOGGER = SqlLog.LOGGER_DB_JDBC;

  /**
   * Exploded variable from {@link TransferSourceTargetOrder}
   */
  private final SqlDataPathRelationDef targetMetaDef;
  private final RelationDef sourceMetaDef;
  private final SqlDataPath targetDataPath;
  private final SqlDataSystem dataSystem;
  private final boolean withSqlParameters;
  private final TransferSourceTargetOrder transferSourceTarget;
  private final UpsertType upsertType;

  /**
   * Statement used if {@link TransferPropertiesSystem#getWithBindVariablesStatement()} is true
   * This is the first one that corresponds to {@link #firstSqlStatement}
   */
  private PreparedStatement firstPreparedStatement;
  /**
   * Statement used if {@link TransferPropertiesSystem#getWithBindVariablesStatement()} is false
   */
  private Statement firstPlainStatement;

  /**
   * Processing variables
   */
  private Connection connection;
  /**
   * The variable that holds the final statement (parametrized or not)
   */
  private String firstSqlStatement;
  /**
   * The type of the first sql statement
   */
  private SqlStatementType firstSqlStatementType;

  /**
   * Batch support processing variable, the support even if asked
   * may be not supported
   */
  private Boolean batchMode;


  /**
   * The list of sqlXmlObject that needs to be freed from memory if any at close time
   */
  private List<SQLXML> sqlXmlObjects = new ArrayList<>();

  /**
   * Operation / Method
   */
  private TransferMethod transferMethod = super.getMethod();
  private final TransferOperation transferOperation;

  /**
   * For a prepared statement, if this is a batch, we will have multiple rows
   * otherwise we get the actual row
   * (for debugging purpose)
   */
  private List<List<?>> actualRows = new ArrayList<>();
  /**
   * For a statement with literal,
   * A list of sql statement in the batch or the actual statement executed
   * (for debugging purpose)
   */
  private List<String> actualSQLStatements = new ArrayList<>();
  /**
   * A second sql in case of failure (for an upsert)
   */
  private String secondSqlStatement;
  /**
   * The second sql type (UPDATE or INSERT normally)
   */
  private SqlStatementType secondSqlStatementType;
  /**
   * Second prepared statement in case of failure
   * (ie update or insert in an OLD fashioned upsert)
   */
  private PreparedStatement secondPreparedStatement;
  private Statement secondPlainStatement;


  private SqlInsertStream(TransferSourceTargetOrder transferSourceTarget) {
    super(transferSourceTarget.getTargetDataPath());
    /**
     * Explode transferSourceTarget into different variables
     */
    this.transferSourceTarget = transferSourceTarget;
    this.targetDataPath = (SqlDataPath) transferSourceTarget.getTargetDataPath();

    TransferPropertiesSystem transferProperties = transferSourceTarget.getTransferProperties();
    this.targetMetaDef = targetDataPath.getOrCreateRelationDef();

    DataPath sourceDataPath = transferSourceTarget.getSourceDataPath();
    /**
     * If the source is not defined, we expect the same structure than the target
     */
    if (sourceDataPath == null) {
      sourceDataPath = targetDataPath;
    }
    this.sourceMetaDef = sourceDataPath.getOrCreateRelationDef();
    this.dataSystem = targetDataPath.getConnection().getDataSystem();
    this.withSqlParameters = transferProperties.getWithBindVariablesStatement();
    this.upsertType = transferProperties.getUpsertType();

    if (transferProperties.getOperation() == null) {
      transferOperation = this.dataSystem.getDefaultTransferOperation();
      SqlLog.LOGGER_DB_JDBC.info("The load operation was not set, taking the default (" + transferOperation + ")");
    } else {
      transferOperation = transferProperties.getOperation();
    }
    preTransfer();
  }

  public synchronized static SqlInsertStream create(TransferSourceTargetOrder transferSourceTarget) {
    DataPath targetDataPath = transferSourceTarget.getTargetDataPath();
    if (!Tabulars.exists(targetDataPath)) {
      throw new RuntimeException("You can't open an insert stream on the SQL table (" + targetDataPath + ") because it does not exist.");
    }
    return new SqlInsertStream(transferSourceTarget);
  }

  @Override
  public InsertStream insert(List<Object> sourceValues) {

    final int columnsSize = this.targetDataPath.getOrCreateRelationDef().getColumnsSize();
    final int valuesSize = sourceValues.size();

    if (valuesSize != columnsSize) {
      LOGGER.fine("The number of values to insert (" + valuesSize + ") is not the same than the number of columns (" + columnsSize + ")");
    }


    try {

      currentRowInLogicalBatch++;

      if (this.batchMode) {

        /**
         * Batch Mode
         */
        if (this.withSqlParameters) {
          prepareStatement(firstPreparedStatement, firstSqlStatementType, sourceValues);
          firstPreparedStatement.addBatch();
        } else {
          String sql = getSqlStatementWithoutParameters(sourceValues, firstSqlStatement, firstSqlStatementType);
          actualSQLStatements.add(sql);
          firstPlainStatement.addBatch(sql);
        }

        // Submit the batch for execution if full
        if (currentRowInLogicalBatch >= this.batchSize) {

          if (this.batchMode) {
            executeBatch();
          }

          if (Math.floorMod(insertStreamListener.getBatchCount(), commitFrequency) == 0) {
            commit();
          }

          // Update the counter
          insertStreamListener.addRows(currentRowInLogicalBatch);

          if (Math.floorMod(insertStreamListener.getBatchCount(), feedbackFrequency) == 0) {
            LOGGER.info(insertStreamListener.getRowCount() + " rows loaded in the table " + targetMetaDef.getDataPath());
          }
          currentRowInLogicalBatch = 0;
        }

        return this;

      }

      /**
       * Not in batch execution mode
       */
      try {
        if (this.withSqlParameters) {
          prepareStatement(firstPreparedStatement, firstSqlStatementType, sourceValues);
          firstPreparedStatement.execute();
          freeSqlXmlObject();
          actualRows = new ArrayList<>();
        } else {
          String sql = getSqlStatementWithoutParameters(sourceValues, firstSqlStatement, firstSqlStatementType);
          this.actualSQLStatements.add(sql);
          firstPlainStatement.execute(sql);
          actualSQLStatements = new ArrayList<>();
        }
        /**
         * If first statement is an update of an upsert
         */
        if (transferOperation == TransferOperation.UPSERT && firstSqlStatementType == SqlStatementType.UPDATE) {
          if (this.withSqlParameters) {
            if (firstPreparedStatement.getUpdateCount() == 0) {
              prepareStatement(secondPreparedStatement, secondSqlStatementType, sourceValues);
              secondPreparedStatement.execute();
              freeSqlXmlObject();
              actualRows = new ArrayList<>();
            }
          } else {
            if (firstPlainStatement.getUpdateCount() == 0) {
              String sql = getSqlStatementWithoutParameters(sourceValues, secondSqlStatement, secondSqlStatementType);
              actualSQLStatements.add(sql);
              secondPlainStatement.execute(sql);
              actualSQLStatements = new ArrayList<>();
            }
          }
        }
      } catch (SQLException e) {
        if (transferOperation != TransferOperation.UPSERT) {
          throw e;
        }
        /**
         * We may get a normal error only on insert/update
         */
        if (upsertType != INSERT_UPDATE) {
          throw e;
        }
        /**
         * We commit to close the actual transaction
         * ie to not have this error.
         * Caused by: org.postgresql.util.PSQLException: ERROR: current transaction is aborted, commands ignored until end of transaction block
         */
        commit();

        /**
         * Delete the first insert or update of the upsert
         */
        actualRows = new ArrayList<>();
        actualSQLStatements = new ArrayList<>();

        /**
         * Second statement execution update or insert
         */
        if (this.withSqlParameters) {
          prepareStatement(secondPreparedStatement, secondSqlStatementType, sourceValues);
          secondPreparedStatement.execute();
          freeSqlXmlObject();
          actualRows = new ArrayList<>();
        } else {
          String sql = getSqlStatementWithoutParameters(sourceValues, secondSqlStatement, secondSqlStatementType);
          actualSQLStatements.add(sql);
          secondPlainStatement.execute(sql);
          actualSQLStatements = new ArrayList<>();
        }
      }


    } catch (SQLException e) {

      // A catch all
      resourceClose();

      String message = "Error on insert on the resource : " + targetMetaDef.getDataPath() + ". Error Message: " + e.getMessage();
      if (!this.batchMode) {
        /**
         * batch error is already caught in {@link #executeBatch()}
         */
        if (!this.actualRows.isEmpty()) {
          message += "\nActual Rows:\n" + actualRows.stream()
            .map(d -> Casts.castToNewListSafe(d, String.class))
            .map(d -> d.stream().map(s -> "'" + s + "'").collect(Collectors.joining(",")))
            .collect(Collectors.joining(System.lineSeparator()));
        }
        if (!this.actualSQLStatements.isEmpty()) {
          message += "\nActual Statements:\n" + actualSQLStatements.stream()
            .collect(Collectors.joining(";" + System.lineSeparator()));
        }
      }
      throw new RuntimeException(message, e);

    }


    return this;

  }

  /**
   * The sql statement without parameters
   * Convert the source values before statement
   * (ie a string yes to a boolean, etc ...)
   */
  private String getSqlStatementWithoutParameters(List<Object> sourceValues, String sqlStatement, SqlStatementType sqlStatementType) {
    /**
     * static for test purpose
     */
    return getSqlStatementWithoutParametersStatic(transferSourceTarget, sqlStatement, sourceValues, sqlStatementType);
  }

  private void prepareStatement(PreparedStatement preparedStatement, SqlStatementType sqlStatementType, List<Object> sourceValues) {
    List<Integer> sourceColumnPositionInStatementOrder = transferSourceTarget.getSourceColumnPositionInStatementOrder(sqlStatementType);
    List<Object> actualRow = new ArrayList<>();
    actualRows.add(actualRow);
    int positionInStatement = 0;
    for (Integer columnPosition : sourceColumnPositionInStatementOrder) {
      positionInStatement++;
      Object sourceObject = sourceValues.get(columnPosition - 1);
      actualRow.add(sourceObject);
      final ColumnDef<?> sourceColumn = sourceMetaDef.getColumnDef(columnPosition);
      final ColumnDef<?> targetColumn;
      try {
        targetColumn = transferSourceTarget.getTargetColumnFromSourceColumn(sourceColumn);
      } catch (NoColumnException e) {
        throw new IllegalStateException("A target column could not be found for the source (" + sourceColumn + ")");
      }
      int targetColumnType = targetColumn.getDataType().getVendorTypeNumber();
      try {
        if (sourceObject != null) {

          Object loadObject = targetDataPath.getConnection().toSqlObject(sourceObject, targetColumn.getDataType());
          if (loadObject instanceof SQLXML) {
            this.sqlXmlObjects.add((SQLXML) loadObject);
          }
          preparedStatement.setObject(positionInStatement, loadObject, targetColumnType);

        } else {

          preparedStatement.setNull(positionInStatement, targetColumnType);

        }
      } catch (Exception e) {
        String sourceObjectClass = "null";
        if (sourceObject != null) {
          sourceObjectClass = sourceObject.getClass().toString();
        }
        String message = e + ", Source Column: " + sourceColumn.getFullyQualifiedName() + " (Class: " + sourceObjectClass + ", Value:" + sourceObject + "),  TargetColumn: " + targetColumn.getFullyQualifiedName() + " (Type: " + targetColumn.getDataType() + ")";
        throw new RuntimeException(message, e);

      }

    }
  }


  /**
   * @param transferSourceTarget - the transfer
   * @param printfStatement      -  the printf expression
   * @param sourceValues         - the source values
   * @return a statement with values
   */
  public static String getSqlStatementWithoutParametersStatic(TransferSourceTargetOrder transferSourceTarget, String printfStatement, List<?> sourceValues, SqlStatementType statementType) {

    List<String> sqlValuesInStatementOrder = new ArrayList<>();
    for (Integer columnPosition : transferSourceTarget.getSourceColumnPositionInStatementOrder(statementType)) {
      Object sourceObject = sourceValues.get(columnPosition - 1);
      final ColumnDef<?> sourceColumn = transferSourceTarget.getSourceDataPath().getOrCreateRelationDef().getColumnDef(columnPosition);
      final ColumnDef<?> targetColumn;
      try {
        targetColumn = transferSourceTarget.getTargetColumnFromSourceColumn(sourceColumn);
      } catch (NoColumnException e) {
        throw new IllegalStateException("A target column could not be found for the source column (" + sourceColumn + ")");
      }

      SqlConnection targetConnection = (SqlConnection) transferSourceTarget.getTargetDataPath().getConnection();


      SqlDataType<?> targetDataType = targetColumn.getDataType();
      Class<?> javaClass = targetDataType.getValueClass();
      String sqlValue;
      if (javaClass.equals(SQLXML.class)) {
        /**
         * SQLXML in literal should be a string, not a {@link SQLXML}
         * otherwise we get the name of the class object
         */
        sqlValue = targetConnection.getObject(sourceObject, String.class);

      } else {

        sqlValue = targetConnection.toSqlString(sourceObject, targetColumn);

      }

      /**
       * Add the quote if it's not null or a number
       */
      if (!targetDataType.isNumber() && sqlValue != null) {
        sqlValue = "'" + sqlValue + "'";
      }


      sqlValuesInStatementOrder.add(sqlValue);
    }
    // the redundant cast is to pass the value as a varargs and not as a single value
    //noinspection RedundantCast
    return String.format(printfStatement, (Object[]) sqlValuesInStatementOrder.toArray(new Object[0]));
  }

  /**
   * Free Xml object created
   * <a href="https://docs.oracle.com/javase/tutorial/jdbc/basics/sqlxml.html#releasing_sqlxml">...</a>
   */
  private void freeSqlXmlObject() {
    try {
      for (SQLXML sqlXmlObject : this.sqlXmlObjects) {
        sqlXmlObject.free();
      }
      this.sqlXmlObjects = new ArrayList<>();
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public SqlDataPath getDataPath() {
    return (SqlDataPath) super.getDataPath();
  }

  /**
   * PreTransfer
   */
  private void preTransfer() {


    if (targetDataPath.getConnection().getMetadata().getMaxWriterConnection() == 1) {
      connection = targetDataPath.getConnection().getCurrentJdbcConnection();
    } else {
      connection = targetDataPath.getConnection().getNewJdbcConnection();
    }

    try {
      this.batchMode = connection.getMetaData().supportsBatchUpdates();
      if (!this.batchMode) {

        LOGGER.warning("The driver is not supporting batch update. The insert would be then slower.");

      } else {

        LOGGER.info("The driver is supporting batch update. ");
        // turn off autocommit for the batch update
        // By default, it's ON
        // See https://docs.oracle.com/javase/7/docs/api/java/sql/Connection.html
        // If the setAutocommit comes after the sourceResultSet, with LONG Oracle you of a stream errors
        // Auto-commit mode indicates to the database whether to issue an automatic COMMIT operation
        // after every SQL operation.
        connection.setAutoCommit(false);

      }
    } catch (SQLException e) {
      this.batchMode = false;
      LOGGER.warning("supportsBatchUpdates: An exception was thrown with the following message: " + e.getMessage());
      LOGGER.warning("supportsBatchUpdates: was set to " + this.batchMode);
    }

    /**
     * It's quicker with, than without
     */
    if (this.withSqlParameters) {
      createPreparedStatement();
    } else {
      createPrintfStatement();
    }


  }


  /**
   * Called when there is no records anymore to add
   */
  @Override
  public void close() {

    // Submit the rest
    if (this.batchMode) {
      executeBatch();
    }

    commit();

    insertStreamListener.addRows(currentRowInLogicalBatch);

    LOGGER.info(insertStreamListener.getRowCount() + " rows loaded (Total) in the table " + targetDataPath);
    LOGGER.info(insertStreamListener.getCommits() + " commit(s) (Total) in the table " + targetDataPath);
    LOGGER.info(insertStreamListener.getBatchCount() + " batches(s) (Total) in the table " + targetDataPath);

    resourceClose();


  }

  /**
   * In case of parent child hierarchy
   * we can check if we need to send the data with the function nextInsertSendBatch()
   * and send it with this function
   */
  @Override
  public void flush() {
    executeBatch();
    commit();
  }

  private void executeBatch() {
    try {
      if (this.currentRowInLogicalBatch != 0) {
        if (this.withSqlParameters) {
          if (firstPreparedStatement != null && !firstPreparedStatement.isClosed()) {
            firstPreparedStatement.executeBatch();
          }
        } else {
          if (firstPlainStatement != null && !firstPlainStatement.isClosed()) {
            firstPlainStatement.executeBatch();
          }
        }
        insertStreamListener.incrementBatch();
        freeSqlXmlObject();
        if (this.withSqlParameters) {
          actualRows = new ArrayList<>();
        } else {
          actualSQLStatements = new ArrayList<>();
        }
      }
    } catch (SQLException e) {

      /**
       * We commit otherwise we will have insert waiting for commit
       * And it will hang a drop in a test suite
       */
      commit();

      String message = "Error on batch execution.\nError Message: " + e.getMessage();

      /**
       * Not all driver returns the correct sql or data that is wrong
       * If the batch size is big we don't see the error
       * We set it for now to 50
       */
      int failedSampleSize = 50;
      if (this.withSqlParameters) {

        List<List<?>> failedRows = new ArrayList<>();
        if (e instanceof BatchUpdateException) {
          // https://stackoverflow.com/questions/11298220/jdbc-batch-insert-exception-handling
          BatchUpdateException exc = (BatchUpdateException) e;
          // If the first 99 statements succeed, the 100th statement generates an error,
          // and the remaining statements are not executed, you should get back a 100 element array where the first 99 elements indicate success and the 100th element indicates Statement.EXECUTE_FAILED.
          int[] updateCounts = exc.getUpdateCounts();
          if (updateCounts.length != 0) {
            for (int i = 0; i < updateCounts.length; i++) {
              if (updateCounts[i] == Statement.EXECUTE_FAILED) {
                failedRows.add(actualRows.get(i));
              }
            }
          }
        }
        if (failedRows.isEmpty()) {
          for (int i = 0; i < failedSampleSize; i++) {
            try {
              failedRows.add(actualRows.get(i));
            } catch (IndexOutOfBoundsException ex) {
              break;
            }
          }
        }
        String errorSample = failedRows.stream()
          .map(d -> Casts.castToNewListSafe(d, String.class))
          .map(d -> d.stream().map(s -> "'" + s + "'").collect(Collectors.joining(",")))
          .collect(Collectors.joining(System.lineSeparator()));
        message += "\nStatement: " + firstSqlStatement + "\nSample Error Rows:\n" + errorSample;
      } else {
        message += "\nSample SQL Error:\n" + this.actualSQLStatements.stream()
          .limit(failedSampleSize)
          .collect(Collectors.joining(System.lineSeparator()));
      }

      throw new RuntimeException(message, e);
    }
  }

  /**
   * Send a manual commit
   */
  public void commit() {
    try {

      if (!connection.getAutoCommit()) {
        connection.commit();
        insertStreamListener.incrementCommit();
        LOGGER.info("commit in the table " + targetDataPath);
      } else {
        throw new RuntimeException("Don't send a commit on a autocommit session");
      }

    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }


  /**
   * Use to close the resource when an error occurs
   * during insertion
   * <p>
   * This chunk of code must never fail
   * <p>
   * The close function calls also this function
   * And as it may be called by the client
   */
  private void resourceClose() {
    try {

      if (firstPreparedStatement != null) {
        if (!firstPreparedStatement.isClosed()) {
          firstPreparedStatement.close();
        }
      }

      if (firstPlainStatement != null) {
        if (!firstPlainStatement.isClosed()) {
          firstPlainStatement.close();
        }
      }

      // Setting this setting to true, resolve the database locked problem
      // but introduce another problem when two streams are running concurrently
      // for instance the child and the parent of a tpc-ds data loading (store_sales, store_returns)
      // When a thread finish before the other one, it's setting the autocommit back to on
      // which slow the loading so much that you are thinking that this is locked
      //
      // Autocommit is also tricky because a select in sqlite in non-autocommit mode
      // will lock the database until a commit is executed
      //
      // Don't set it back to true here
      // TODO: A unit of SqlInsertStream loading (autocommit is needed to not have a lock after a select on sqlite)
      // [SQLITE_BUSY]  The database file is locked (database is locked)
      if (!connection.getAutoCommit()) {
        connection.setAutoCommit(true);
      }

      // Connection close
      final SqlDataPath dataPath = targetMetaDef.getDataPath();
      if (!connection.equals(dataPath.getConnection().getCurrentJdbcConnection())) {
        connection.close();
      }

      LOGGER.fine(getName() + " stream closed");

    } catch (SQLException e) {
      throw new RuntimeException(e);
    }

  }


  /**
   * This function is called with {@link TransferPropertiesSystem#getWithBindVariablesStatement()} is true
   */
  private void createPreparedStatement() {
    // Named parameters / bind variables
    SqlConnection targetDataStore = this.getDataPath().getConnection();
    boolean supportsNamedParameters = targetDataStore.getMetadata().supportsSqlParameters();
    if (!supportsNamedParameters) {
      SqlLog.LOGGER_DB_JDBC.warning("The datastore (" + targetDataStore + ") does not support SQL parameters, the transfer will be done without and will then be slower");
      createPrintfStatement();
    } else {
      /**
       * The statement
       */
      switch (transferOperation) {
        case INSERT:
        case COPY:
          firstSqlStatement = dataSystem.createInsertStatementWithBindVariables(transferSourceTarget);
          firstSqlStatementType = SqlStatementType.INSERT;
          LOGGER.info("Insert Statement: " + firstSqlStatement);
          this.transferMethod = TransferMethod.INSERT_WITH_BIND_VARIABLE;
          break;
        case UPSERT:
          UpsertType upsertType = this.upsertType;
          /**
           * If the target has no unique constraint, we insert
           */
          if (transferSourceTarget.getTargetUniqueColumns().isEmpty()) {
            upsertType = INSERT;
          } else {
            /**
             * If the merge statement is not implemented (default)
             * We do an insert/update
             */
            String mergeStatementWithBindVariables = dataSystem.createUpsertMergeStatementWithParameters(transferSourceTarget);
            if (upsertType == UpsertType.MERGE && mergeStatementWithBindVariables == null || mergeStatementWithBindVariables.isBlank()) {
              Long count = getDataPath().getCount();
              if (count == 0) {
                upsertType = INSERT;
              } else {
                upsertType = UPDATE_INSERT;
              }
            }
          }
          switch (upsertType) {
            case INSERT:
              firstSqlStatement = dataSystem.createInsertStatementWithBindVariables(transferSourceTarget);
              firstSqlStatementType = SqlStatementType.INSERT;
              this.transferMethod = TransferMethod.INSERT_WITH_BIND_VARIABLE;
              break;
            case MERGE:
              firstSqlStatement = dataSystem.createUpsertMergeStatementWithParameters(transferSourceTarget);
              firstSqlStatementType = SqlStatementType.MERGE;
              LOGGER.info("Upsert Merge Statement: " + firstSqlStatement);
              this.transferMethod = TransferMethod.UPSERT_MERGE_WITH_PARAMETERS;
              break;
            case UPDATE_INSERT:
              firstSqlStatement = dataSystem.createUpdateStatementWithBindVariables(transferSourceTarget);
              firstSqlStatementType = SqlStatementType.UPDATE;
              secondSqlStatement = dataSystem.createInsertStatementWithBindVariables(transferSourceTarget);
              secondSqlStatementType = SqlStatementType.INSERT;
              this.transferMethod = TransferMethod.UPSERT_UPDATE_INSERT_WITH_PARAMETERS;
              // we can't update in batch as we need to handle any error
              this.batchMode = false;
              break;
            case INSERT_UPDATE:
              firstSqlStatement = dataSystem.createInsertStatementWithBindVariables(transferSourceTarget);
              firstSqlStatementType = SqlStatementType.INSERT;
              secondSqlStatement = dataSystem.createUpdateStatementWithBindVariables(transferSourceTarget);
              secondSqlStatementType = SqlStatementType.UPDATE;
              this.transferMethod = TransferMethod.UPSERT_INSERT_UPDATE_WITH_PARAMETERS;
              // we can't update in batch as we need to handle any error
              this.batchMode = false;
              break;
            default:
              throw new MissingSwitchBranch("upsertType", upsertType);
          }
          break;
        case UPDATE:
          firstSqlStatement = dataSystem.createUpdateStatementWithBindVariables(transferSourceTarget);
          firstSqlStatementType = SqlStatementType.UPDATE;
          LOGGER.info("Update Statement: " + firstSqlStatement);
          this.transferMethod = TransferMethod.UPDATE_WITH_BIND_VARIABLE;
          break;
        case DELETE:
          firstSqlStatement = dataSystem.createDeleteStatementWithBindVariables(transferSourceTarget);
          firstSqlStatementType = SqlStatementType.DELETE;
          LOGGER.info("Delete Statement: " + firstSqlStatement);
          this.transferMethod = TransferMethod.DELETE_WITH_BIND_VARIABLE;
          break;
        default:
          throw new UnsupportedOperationException("The transfer operation (" + transferOperation + ") is not yet supported on prepared statement");
      }
      try {
        firstPreparedStatement = connection.prepareStatement(firstSqlStatement);
        if (secondSqlStatement != null) {
          secondPreparedStatement = connection.prepareStatement(secondSqlStatement);
        }
      } catch (SQLException e) {
        throw new RuntimeException(e);
      }
    }
  }

  /**
   * This function is called when {@link TransferPropertiesSystem#getWithBindVariablesStatement()} is false
   */
  private void createPrintfStatement() {
    try {
      firstPlainStatement = connection.createStatement();

      /**
       * The sql statement include the values and is then created on the fly
       */
      switch (transferOperation) {
        case INSERT:
        case COPY:
          this.firstSqlStatement = dataSystem.createInsertStatementWithPrintfExpressions(transferSourceTarget);
          this.transferMethod = TransferMethod.INSERT;
          this.firstSqlStatementType = SqlStatementType.INSERT;
          LOGGER.info("Insert Statement: " + firstSqlStatement);
          break;
        case UPSERT:
          UpsertType upsertType = this.upsertType;
          /**
           * If the merge statement is not implemented (default)
           * We do an insert/update
           */
          String upsertMergeStatementWithPrintfExpressions = dataSystem.createUpsertMergeStatementWithPrintfExpressions(transferSourceTarget);
          if (upsertType == UpsertType.MERGE && upsertMergeStatementWithPrintfExpressions == null || upsertMergeStatementWithPrintfExpressions.isBlank()) {
            Long count = getDataPath().getCount();
            if (count == 0) {
              upsertType = INSERT_UPDATE;
            } else {
              upsertType = UPDATE_INSERT;
            }
          }
          switch (upsertType) {
            case MERGE:
              firstSqlStatement = upsertMergeStatementWithPrintfExpressions;
              firstSqlStatementType = SqlStatementType.MERGE;
              LOGGER.info("Upsert Merge Statement: " + firstSqlStatement);
              this.transferMethod = TransferMethod.UPSERT_MERGE_LITERAL;
              break;
            case UPDATE_INSERT:
              firstSqlStatement = dataSystem.createUpdateStatementWithPrintfExpressions(transferSourceTarget);
              firstSqlStatementType = SqlStatementType.UPDATE;
              secondPlainStatement = connection.createStatement();
              secondSqlStatement = dataSystem.createInsertStatementWithPrintfExpressions(transferSourceTarget);
              secondSqlStatementType = SqlStatementType.INSERT;
              this.transferMethod = TransferMethod.UPSERT_UPDATE_INSERT_WITHOUT_PARAMETERS;
              // we can't update in batch as we need to handle any error
              this.batchMode = false;
              break;
            case INSERT_UPDATE:
              firstSqlStatement = dataSystem.createInsertStatementWithPrintfExpressions(transferSourceTarget);
              firstSqlStatementType = SqlStatementType.INSERT;
              secondPlainStatement = connection.createStatement();
              secondSqlStatement = dataSystem.createUpdateStatementWithPrintfExpressions(transferSourceTarget);
              secondSqlStatementType = SqlStatementType.UPDATE;
              this.transferMethod = TransferMethod.UPSERT_INSERT_UPDATE_WITHOUT_PARAMETERS;
              // we can't update in batch as we need to handle any error
              this.batchMode = false;
              break;
            default:
              throw new MissingSwitchBranch("upsertType", upsertType);
          }
          break;
        case UPDATE:
          this.firstSqlStatement = dataSystem.createUpdateStatementWithPrintfExpressions(transferSourceTarget);
          this.transferMethod = TransferMethod.UPDATE;
          firstSqlStatementType = SqlStatementType.UPDATE;
          LOGGER.info("Update Statement: " + firstSqlStatement);
          break;
        case DELETE:
          firstSqlStatement = dataSystem.createDeleteStatementWithPrintfExpressions(transferSourceTarget);
          this.transferMethod = TransferMethod.DELETE;
          firstSqlStatementType = SqlStatementType.DELETE;
          LOGGER.info("Delete Statement: " + firstSqlStatement);
          break;
        default:
          throw new UnsupportedOperationException("The transfer operation (" + transferOperation + ") is not yet supported with a sql statement transfer with values");
      }
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public TransferMethod getMethod() {
    return this.transferMethod;
  }
}
