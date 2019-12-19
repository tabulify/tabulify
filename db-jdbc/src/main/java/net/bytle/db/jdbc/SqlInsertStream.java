package net.bytle.db.jdbc;

import net.bytle.db.DbLoggers;
import net.bytle.db.model.ColumnDef;
import net.bytle.db.model.RelationDef;
import net.bytle.db.model.TableDef;
import net.bytle.db.spi.Tabulars;
import net.bytle.db.stream.InsertStream;
import net.bytle.db.stream.InsertStreamAbs;
import net.bytle.log.Log;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

public class SqlInsertStream extends InsertStreamAbs implements InsertStream, AutoCloseable {

  public static final Log LOGGER = DbLoggers.LOGGER_DB_ENGINE;
  private final TableDef targetMetaDef;
  private final JdbcDataPath jdbcDataPath;

  private PreparedStatement preparedStatement;
  private Connection connection;
  private String insertStatement;
  private RelationDef sourceMetaDef;
  private Boolean supportBatch;
  private Boolean supportNamedParameters;
  private Statement statement;

  private SqlInsertStream(JdbcDataPath jdbcDataPath) {
    super(jdbcDataPath);
    this.jdbcDataPath = jdbcDataPath;
    this.targetMetaDef = jdbcDataPath.getDataDef();
    init();
  }

  public synchronized static SqlInsertStream of(JdbcDataPath jdbcDataPath) {
    if (!Tabulars.exists(jdbcDataPath)) {
      throw new RuntimeException("You can't open an insert stream on the SQL table (" + jdbcDataPath + ") because it does not exist.");
    }
    return new SqlInsertStream(jdbcDataPath);

  }

  @Override
  public InsertStream insert(List<Object> values) {

    final int columnsSize = this.jdbcDataPath.getDataDef().getColumnDefs().size();
    final int valuesSize = values.size();
    assert valuesSize == columnsSize : "The number of values to insert (" + valuesSize + ") is not the same than the number of columns (" + columnsSize + ")";

    if (this.supportNamedParameters) {

      try {
        // Columns
        for (int i = 0; i < valuesSize; i++) {

          Object sourceObject = values.get(i);
          final ColumnDef column = sourceMetaDef.getColumnDef(i);
          int targetColumnType = column.getDataType().getTypeCode();
          try {
            if (sourceObject != null) {

              Object loadObject = Jdbcs.castLoadObjectIfNecessary(preparedStatement.getConnection(), targetColumnType, sourceObject);
              preparedStatement.setObject(i + 1, loadObject, targetColumnType);


            } else {

              preparedStatement.setNull(i + 1, targetColumnType);

            }
          } catch (Exception e) {

            String message = e + ", Object:" + sourceObject.getClass() + ", Value:" + sourceObject + ", columnDataType:" + targetColumnType + " Column:" + column.getFullyQualifiedName();
            throw new RuntimeException(message, e);

          }

        }

        currentRowInLogicalBatch++;

        if (this.supportBatch) {

          preparedStatement.addBatch();

        } else {

          preparedStatement.execute();

        }
      } catch (SQLException e) {

        resourceClose();
        throw new RuntimeException("Table: " + targetMetaDef.getDataPath(), e);

      }

    } else {

      try {
        insertStatement = DbDml.getInsertStatement(sourceMetaDef, targetMetaDef, values);
        statement.execute(insertStatement);
        currentRowInLogicalBatch++;
      } catch (SQLException e) {

        resourceClose();
        throw new RuntimeException("Table: " + targetMetaDef.getDataPath(), e);

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

  @Override
  public JdbcDataPath getDataPath() {
    return (JdbcDataPath) super.getDataPath();
  }

  private void init() {

    if (jdbcDataPath.getDataSystem().getMaxWriterConnection() == 1) {
      connection = jdbcDataPath.getDataSystem().getCurrentConnection();
    } else {
      connection = jdbcDataPath.getDataSystem().getNewConnection("InsertStream Table " + jdbcDataPath);
    }
    if (sourceMetaDef == null) {
      sourceMetaDef = targetMetaDef;
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

    // Named parameters / bind variables
    try {
      this.supportNamedParameters = connection.getMetaData().supportsNamedParameters();
      if (!this.supportNamedParameters) {
        LOGGER.warning("The driver is not supporting named parameters. This would slow the insert/update.");
      } else {
        LOGGER.info("The driver is supporting named parameters.");
      }
    } catch (SQLException e) {
      this.supportNamedParameters = false;
      LOGGER.warning("supportsNamedParameters: An exception was thrown with the following message: " + e.getMessage());
      LOGGER.warning("supportsNamedParameters: was set to " + this.supportNamedParameters);
    }

    try {

      if (this.supportNamedParameters) {
        insertStatement = DbDml.getParameterizedInsertStatement(targetMetaDef, sourceMetaDef);
        LOGGER.info("Insert Statement:" + insertStatement);
        preparedStatement = connection.prepareStatement(insertStatement);
      } else {
        statement = connection.createStatement();
      }

    } catch (SQLException e) {

      throw new RuntimeException(e);

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


      LOGGER.info(insertStreamListener.getRowCount() + " rows loaded (Total) in the table " + jdbcDataPath);
      LOGGER.info(insertStreamListener.getCommits() + " commit(s) (Total) in the table " + jdbcDataPath);
      LOGGER.info(insertStreamListener.getBatchCount() + " batches(s) (Total) in the table " + jdbcDataPath);

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
      }
    } catch (SQLException e) {
      throw new RuntimeException(e);
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
        LOGGER.info("commit in the table " + jdbcDataPath);
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

      if (statement != null) {
        if (!statement.isClosed()) {
          statement.close();
        }
      }

      final JdbcDataPath dataPath = (JdbcDataPath) targetMetaDef.getDataPath();
      if (dataPath.getDataSystem().getMaxWriterConnection() > 1) {
        if (connection != null) {
          connection.close();
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
      // SQLITE_BUSY]  The database file is locked (database is locked)
      if (!connection.getAutoCommit()) {
        connection.setAutoCommit(true);
      }
      //

      LOGGER.info(getName() + " stream closed");
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }

  }

  /**
   * The data inserted may be in an other order
   * For instance with a query
   * You may defined the order of the columns by giving a relationDef
   * with the column ordered
   *
   * @param dataDef
   * @return
   */
  public SqlInsertStream setSourceDataDef(RelationDef dataDef) {
    this.sourceMetaDef = dataDef;
    return this;
  }

}
