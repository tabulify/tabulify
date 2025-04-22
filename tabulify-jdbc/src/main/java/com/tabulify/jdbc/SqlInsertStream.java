package com.tabulify.jdbc;

import com.tabulify.model.ColumnDef;
import com.tabulify.model.RelationDef;
import com.tabulify.spi.DataPath;
import com.tabulify.spi.Tabulars;
import com.tabulify.stream.InsertStream;
import com.tabulify.stream.InsertStreamAbs;
import com.tabulify.transfer.TransferMethod;
import com.tabulify.transfer.TransferOperation;
import com.tabulify.transfer.TransferProperties;
import com.tabulify.transfer.TransferSourceTarget;
import net.bytle.exception.NoColumnException;
import net.bytle.log.Log;
import net.bytle.type.Strings;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SqlInsertStream extends InsertStreamAbs implements InsertStream, AutoCloseable {

  public static final Log LOGGER = SqlLog.LOGGER_DB_JDBC;

  /**
   * Exploded variable from {@link TransferSourceTarget}
   */
  private final SqlRelationDef targetMetaDef;
  private final RelationDef sourceMetaDef;
  private final SqlDataPath targetDataPath;
  private final SqlDataSystem dataSystem;
  private final boolean withSqlParameters;
  private final TransferSourceTarget transferSourceTarget;

  /**
   * Statement used if {@link TransferProperties#setWithBindVariablesStatement(Boolean)} is true
   */
  private PreparedStatement preparedStatement;
  /**
   * Statement used if {@link TransferProperties#setWithBindVariablesStatement(Boolean)} is false
   */
  private Statement plainStatement;

  /**
   * Processing variables
   */
  private Connection connection;
  /**
   * The variable that holds the final statement (parametrized or not)
   */
  private String sqlStatement;


  /**
   * Batch support processing variable, the support even if asked
   * may be not supported
   */
  private Boolean supportBatch;


  /**
   * The list of sqlXmlObject that needs to be freed from memory if any at close time
   */
  private List<SQLXML> sqlXmlObjects = new ArrayList<>();

  /**
   * Operation / Method
   */
  private TransferMethod transferMethod = super.getMethod();
  private final TransferOperation transferOperation;


  private SqlInsertStream(TransferSourceTarget transferSourceTarget) {
    super(transferSourceTarget.getTargetDataPath());
    /**
     * Explode transferSourceTarget into different variables
     */
    this.transferSourceTarget = transferSourceTarget;
    this.targetDataPath = (SqlDataPath) transferSourceTarget.getTargetDataPath();

    TransferProperties transferProperties = transferSourceTarget.getTransferProperties();
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
    this.withSqlParameters = transferProperties.withBindVariablesStatement();

    if (transferProperties.getOperation() == null) {
      transferOperation = this.dataSystem.getDefaultTransferOperation();
      SqlLog.LOGGER_DB_JDBC.info("The load operation was not set, taking the default (" + transferOperation + ")");
    } else {
      transferOperation = transferProperties.getOperation();
    }
    preTransfer();
  }

  public synchronized static SqlInsertStream create(TransferSourceTarget transferSourceTarget) {
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

    if (this.withSqlParameters) {

      try {
        // Columns
        int positionInStatement = 0;
        List<Integer> sourceColumnPositionInStatementOrder = transferSourceTarget.getSourceColumnPositionInStatementOrder();
        for (Integer columnPosition : sourceColumnPositionInStatementOrder) {
          positionInStatement++;
          Object sourceObject = sourceValues.get(columnPosition - 1);
          final ColumnDef sourceColumn = sourceMetaDef.getColumnDef(columnPosition);
          final ColumnDef targetColumn;
          try {
            targetColumn = transferSourceTarget.getTargetColumnFromSourceColumn(sourceColumn);
          } catch (NoColumnException e) {
            throw new IllegalStateException("A target column could not be found for the source (" + sourceColumn + ")");
          }
          int targetColumnType = targetColumn.getDataType().getTargetTypeCode();
          try {
            if (sourceObject != null) {

              Object loadObject = targetDataPath.getConnection().toSqlObject(sourceObject, targetColumn.getDataType());
              if (sourceColumn.getDataType().getSqlClass().equals(java.sql.SQLXML.class)) {
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

        currentRowInLogicalBatch++;

        if (this.supportBatch) {

          preparedStatement.addBatch();

        } else {

          preparedStatement.execute();
          freeSqlXmlObject();

        }
      } catch (SQLException e) {

        resourceClose();
        throw new RuntimeException("Table: " + targetMetaDef.getDataPath(), e);

      }

    } else {

      String sql = formatValuesStatement(transferSourceTarget, sqlStatement, sourceValues);
      try {
        plainStatement.execute(sql);
        currentRowInLogicalBatch++;
      } catch (SQLException e) {

        resourceClose();
        throw new RuntimeException("Insertion error with the the insert statement:" + Strings.EOL + sql, e);

      }

    }


    // Submit the batch for execution if full
    if (currentRowInLogicalBatch >= this.batchSize) {

      if (this.supportBatch) {
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
   * @param transferSourceTarget - the transfer
   * @param printfStatement      -  the printf expression
   * @param sourceValues         - the source values
   * @return a statement with values
   */
  public static String formatValuesStatement(TransferSourceTarget transferSourceTarget, String printfStatement, List<?> sourceValues) {
    List<String> sqlValues = new ArrayList<>();
    for (Integer columnPosition : transferSourceTarget.getSourceColumnPositionInStatementOrder()) {
      Object sourceObject = sourceValues.get(columnPosition - 1);
      final ColumnDef sourceColumn = transferSourceTarget.getSourceDataPath().getOrCreateRelationDef().getColumnDef(columnPosition);
      final ColumnDef targetColumn;
      try {
        targetColumn = transferSourceTarget.getTargetColumnFromSourceColumn(sourceColumn);
      } catch (NoColumnException e) {
        throw new IllegalStateException("A target column could not be found for the source column (" + sourceColumn + ")");
      }
      SqlConnection dataStore = (SqlConnection) transferSourceTarget.getTargetDataPath().getConnection();
      String sqlValue = dataStore.toSqlString(sourceObject, targetColumn.getDataType());
      if (!targetColumn.getDataType().isNumeric() && sqlValue != null) {
        sqlValue = "'" + sqlValue + "'";
      }
      sqlValues.add(sqlValue);
    }
    // the redundant cast is to pass the value as a varargs and not as a single value
    //noinspection RedundantCast
    return String.format(printfStatement, (Object[]) sqlValues.toArray(new Object[0]));
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
      connection = targetDataPath.getConnection().getCurrentConnection();
    } else {
      connection = targetDataPath.getConnection().getNewConnection();
    }

    try {
      this.supportBatch = connection.getMetaData().supportsBatchUpdates();
      if (!this.supportBatch) {

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
      this.supportBatch = false;
      LOGGER.warning("supportsBatchUpdates: An exception was thrown with the following message: " + e.getMessage());
      LOGGER.warning("supportsBatchUpdates: was set to " + this.supportBatch);
    }

    /**
     * It's quicker with, than without
     */
    if (this.withSqlParameters) {
      createPreparedStatement();
    } else {
      createStatement();
    }


  }


  /**
   * Called when there is no records anymore to add
   */
  @Override
  public void close() {
    // Submit the rest
    try {

      if (preparedStatement != null) {
        if (!preparedStatement.isClosed()) {
          executeBatch();
        }
      }


      commit();

      insertStreamListener.addRows(currentRowInLogicalBatch);


      LOGGER.info(insertStreamListener.getRowCount() + " rows loaded (Total) in the table " + targetDataPath);
      LOGGER.info(insertStreamListener.getCommits() + " commit(s) (Total) in the table " + targetDataPath);
      LOGGER.info(insertStreamListener.getBatchCount() + " batches(s) (Total) in the table " + targetDataPath);

      resourceClose();

    } catch (SQLException e) {
      throw new RuntimeException(e);
    }

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
        preparedStatement.executeBatch();
        insertStreamListener.incrementBatch();
        freeSqlXmlObject();
      }
    } catch (SQLException e) {
      String statement = sqlStatement;
      throw new RuntimeException("Error: " + e.getMessage() + " on batch execution with statement " + statement, e);
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
   * Use to close the resource when an errors occurs
   * during insertion
   * <p>
   * This chunk of code must never failed
   * <p>
   * The close function calls also this function
   * And as it may be called by the client
   */
  private void resourceClose() {
    try {

      if (preparedStatement != null) {
        if (!preparedStatement.isClosed()) {
          preparedStatement.close();
        }
      }

      if (plainStatement != null) {
        if (!plainStatement.isClosed()) {
          plainStatement.close();
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
      if (!connection.equals(dataPath.getConnection().getCurrentConnection())) {
        connection.close();
      }

      LOGGER.fine(getName() + " stream closed");

    } catch (SQLException e) {
      throw new RuntimeException(e);
    }

  }


  /**
   * This function is called with {@link TransferProperties#withBindVariablesStatement()} is true
   */
  private void createPreparedStatement() {
    // Named parameters / bind variables
    SqlConnection targetDataStore = this.getDataPath().getConnection();
    boolean supportsNamedParameters = targetDataStore.getMetadata().supportsSqlParameters();
    if (!supportsNamedParameters) {
      SqlLog.LOGGER_DB_JDBC.warning("The datastore (" + targetDataStore + ") does not support SQL parameters, the transfer will be done without and will then be slower");
      createStatement();
    } else {
      /**
       * The statement
       */
      switch (transferOperation) {
        case INSERT:
        case COPY:
          sqlStatement = dataSystem.createInsertStatementWithBindVariables(transferSourceTarget);
          LOGGER.info("Insert Statement: " + sqlStatement);
          this.transferMethod = TransferMethod.INSERT_WITH_BIND_VARIABLE;
          break;
        case UPSERT:
          sqlStatement = dataSystem.createUpsertStatementWithBindVariables(transferSourceTarget);
          LOGGER.info("Upsert Statement: " + sqlStatement);
          this.transferMethod = TransferMethod.UPSERT_WITH_BIND_VARIABLE;
          break;
        case UPDATE:
          sqlStatement = dataSystem.createUpdateStatementWithBindVariables(transferSourceTarget);
          LOGGER.info("Update Statement: " + sqlStatement);
          this.transferMethod = TransferMethod.UPDATE_WITH_BIND_VARIABLE;
          break;
        case DELETE:
          sqlStatement = dataSystem.createDeleteStatementWithBindVariables(transferSourceTarget);
          LOGGER.info("Delete Statement: " + sqlStatement);
          this.transferMethod = TransferMethod.DELETE_WITH_BIND_VARIABLE;
          break;
        default:
          throw new UnsupportedOperationException("The transfer operation (" + transferOperation + ") is not yet supported on prepared statement");
      }
      try {
        preparedStatement = connection.prepareStatement(sqlStatement);
      } catch (SQLException e) {
        throw new RuntimeException(e);
      }
    }
  }

  /**
   * This function is called with {@link TransferProperties#withBindVariablesStatement()} is false
   */
  private void createStatement() {
    try {
      plainStatement = connection.createStatement();

      /**
       * The sql statement include the values and is then created on the fly
       *
       */
      switch (transferOperation) {
        case INSERT:
        case COPY:
          this.sqlStatement = dataSystem.createInsertStatementWithPrintfExpressions(transferSourceTarget);
          this.transferMethod = TransferMethod.INSERT;
          LOGGER.info("Insert Statement: " + sqlStatement);
          break;
        case UPSERT:
          this.sqlStatement = dataSystem.createUpsertStatementWithPrintfExpressions(transferSourceTarget);
          this.transferMethod = TransferMethod.UPSERT;
          LOGGER.info("Upsert Statement: " + sqlStatement);
          break;
        case UPDATE:
          this.sqlStatement = dataSystem.createUpdateStatementWithPrintfExpressions(transferSourceTarget);
          this.transferMethod = TransferMethod.UPDATE;
          LOGGER.info("Update Statement: " + sqlStatement);
          break;
        case DELETE:
          sqlStatement = dataSystem.createDeleteStatementWithPrintfExpressions(transferSourceTarget);
          this.transferMethod = TransferMethod.DELETE;
          LOGGER.info("Delete Statement: " + sqlStatement);
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
