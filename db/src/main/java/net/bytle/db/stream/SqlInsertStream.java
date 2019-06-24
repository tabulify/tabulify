package net.bytle.db.stream;

import net.bytle.db.DbLoggers;
import net.bytle.db.database.Database;
import net.bytle.db.database.Databases;
import net.bytle.db.engine.DbDml;
import net.bytle.db.engine.Relations;
import net.bytle.db.engine.Tables;
import net.bytle.db.model.ColumnDef;
import net.bytle.db.model.RelationDef;
import net.bytle.db.model.TableDef;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.logging.Logger;

public class SqlInsertStream extends InsertStreamAbs implements InsertStream {

    public static final Logger LOGGER = DbLoggers.LOGGER_DB_ENGINE;

    private PreparedStatement preparedStatement;
    private Connection connection;
    private String insertStatement;
    private RelationDef dataDef;
    private Boolean supportBatch;
    private Boolean supportNamedParameters;
    private Statement statement;

    private SqlInsertStream(TableDef tableDef) {
        this.tableDef = tableDef;

    }

    public synchronized static SqlInsertStream get(TableDef tableDef) {
        return new SqlInsertStream(tableDef);
    }

    @Override
    public InsertStream insert(List<Object> values) {

        if (connection == null) {
            init();
        }

        try {
            if (this.supportNamedParameters) {


                // Columns
                for (int i = 0; i < values.size(); i++) {

                    Object sourceObject = values.get(i);
                    final ColumnDef column = dataDef.getColumnDef(i);
                    int targetColumnType = column.getDataType().getTypeCode();
                    try {
                        if (sourceObject != null) {

                            Object loadObject = Databases.castLoadObjectIfNecessary(preparedStatement.getConnection(), targetColumnType, sourceObject);
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


            } else {

                insertStatement = DbDml.getInsertStatement(tableDef, dataDef, values);
                statement.execute(insertStatement);
                currentRowInLogicalBatch++;

            }

        } catch (SQLException e) {

            resourceClose();
            throw new RuntimeException("Table: " + tableDef.getFullyQualifiedName(), e);

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
                LOGGER.info(insertStreamListener.getRowCount() + " rows loaded in the table " + tableDef.getFullyQualifiedName());
            }
            currentRowInLogicalBatch = 0;
        }

        return this;

    }

    private void init() {

        final Database database = tableDef.getDatabase();

        if (!Tables.exists(tableDef)) {
            if (tableDef.getColumnDefs().size() == 0) {
                Relations.addColumns(tableDef, dataDef);
            }
            Tables.create(tableDef);
        }

        if (database.getMaxWriterConnection() == 1) {
            connection = database.getCurrentConnection();
        } else {
            connection = database.getNewConnection("InsertStream Table " + tableDef.getName());
        }
        if (dataDef == null) {
            dataDef = tableDef;
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
                // If the setAutocommit comes after the sourceResultSet, with LONG Oracle you get a stream errors
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
                insertStatement = DbDml.getParameterizedInsertStatement(tableDef, dataDef);
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


            LOGGER.info(insertStreamListener.getRowCount() + " rows loaded (Total) in the table " + tableDef.getFullyQualifiedName());
            LOGGER.info(insertStreamListener.getCommits() + " commit(s) (Total) in the table " + tableDef.getFullyQualifiedName());
            LOGGER.info(insertStreamListener.getBatchCount() + " batches(s) (Total) in the table " + tableDef.getFullyQualifiedName());

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
            if (preparedStatement!=null) {
                if (preparedStatement.getConnection()!=null) {
                    preparedStatement.getConnection().commit();
                }
            }

            // statement may be null if there is an error
            if (statement != null){
                if (!statement.isClosed()) {
                    if (statement.getConnection() != null) {
                        statement.getConnection().commit();
                    }
                }
            }
            insertStreamListener.incrementCommit();
            LOGGER.info("commit in the table " + tableDef.getFullyQualifiedName());
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

            if (statement != null){
                if (!statement.isClosed()){
                    statement.close();
                }
            }

            if (tableDef.getDatabase().getMaxWriterConnection() > 1) {
                if (connection != null) {
                    connection.close();
                }
            }


            LOGGER.fine(getName() + " stream closed");
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
    public SqlInsertStream setDataDef(RelationDef dataDef) {
        this.dataDef = dataDef;
        return this;
    }
}
