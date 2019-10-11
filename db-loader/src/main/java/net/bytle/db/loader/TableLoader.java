package net.bytle.db.loader;

import net.bytle.db.database.Database;
import net.bytle.db.engine.DbDml;
import net.bytle.db.engine.Tables;
import net.bytle.db.model.DbObjectBuilder;
import net.bytle.db.model.TableDef;

import java.security.InvalidParameterException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;


/**
 * Created by gerard on 9/17/2015.
 * A data loader: Load data (a list of object) in a Database table through JDBC
 */
public class TableLoader implements AutoCloseable {

    private static final Logger LOGGER = Logger.getLogger(Thread.currentThread().getStackTrace()[0].getClassName());

    private final Database database;

    // Atomic Boolean to signal that the producer work is done
    // (Set within the close method to true)
    private AtomicBoolean producerWorkIsDone = new AtomicBoolean(false);
    ;

    // The queue (the buffer) that will be holding the data between the producer and the consumer
    BlockingQueue<Object[]> queue;

    // The thread target executor that will manage the (consumer|target) thread
    ExecutorService targetWorkExecutor;

    // The default target table name if no name is given
    public String targetTableName;

    // Table attributes that will change the create statement
    public Properties tableAttributes;

    // The loading options
    private Set<TableLoaderOption> tableLoaderOptions = new HashSet<>();

    // The default loading parameters
    // Batch Size is used in the consumer side (when inserting)
    private int batchSize;
    // Number of consumer thread
    private Integer targetWorkerCount;
    // Time out
    private long timeout;

    // The table name if no table is given
    private TableDef tableDef;



    /**
     *
     */
    public TableLoader(Builder builder) {


        this.database = builder.database;
        this.batchSize = builder.batchSize;

        this.tableDef = builder.tableDef;
        if (builder.tableLoaderOptions.size()==0){
            this.tableLoaderOptions.add(TableLoaderOptions.CREATE_TABLE_IF_NOT_EXIST);
            this.tableLoaderOptions.add(TableLoaderOptions.INSERT_RECORDS);
        } else {
            this.tableLoaderOptions = builder.tableLoaderOptions;

            // Upsert becomes merge
            if (this.tableLoaderOptions.contains(TableLoaderOptions.UPSERT_RECORDS)){
                this.tableLoaderOptions.remove(TableLoaderOptions.UPSERT_RECORDS);
                this.tableLoaderOptions.add(TableLoaderOptions.MERGE_RECORDS);
            }
            if (builder.mergeColumnPositions.size()!=0 && !this.tableLoaderOptions.contains(TableLoaderOptions.UPDATE_RECORDS)){
                this.tableLoaderOptions.add(TableLoaderOptions.MERGE_RECORDS);
            }
            // If their is no loading option, insert
            if (!(this.tableLoaderOptions.contains(TableLoaderOptions.UPSERT_RECORDS)|| this.tableLoaderOptions.contains(TableLoaderOptions.INSERT_RECORDS) || this.tableLoaderOptions.contains(TableLoaderOptions.UPDATE_RECORDS))) {
                this.tableLoaderOptions.add(TableLoaderOptions.INSERT_RECORDS);
            }


        }

        this.targetWorkerCount = builder.targetWorkerCount;
        this.timeout = builder.timeout;
        this.batchSize = builder.batchSize;

        // TODO: All the below need to be in the builder build method...
        // A row is just a list of objects
        Integer bufferSize = targetWorkerCount * batchSize * 2;
        queue = new ArrayBlockingQueue<>(bufferSize);

        try {
            /**
             * Not every database can make a lot of connection
             * We get the last connection object for single connection database such as sqlite.
             *
             * Example:
             *     * their is already a connection through a select for instance
             *     * and that the database does not support multiple connection (such as Sqlite)
             **/
            Connection targetConnection = database.getCurrentConnection();

            // One connection is already used in the construction of the database
            if (targetWorkerCount > database.getMaxWriterConnection() ) {
                throw new IllegalArgumentException("The database (" + database.getDatabaseProductName() + ") does not support more than (" + database.getMaxWriterConnection() + ") connections. We can then not start (" + targetWorkerCount + ") workers. (1) connection is also in use.");
            }

            // Target table
            if (tableDef==null) {
                // A table with one column
                tableDef = database.getTable("test");
            }
            // Get a table with the good data type
            tableDef = DbObjectBuilder.cleanTableDef(tableDef);

            /**
             * Loading preparation
             *   * Creation of the table
             *   * Creation of the statement
             */
            Boolean tableExist = Tables.exists(tableDef);

            // A statement to drop or create the table
            Statement statement = targetConnection.createStatement();

            // Go ahead if the table Exist
            if (tableExist) {
                if (this.tableLoaderOptions.contains(TableLoaderOptions.CREATE_TABLE) ) {
                    throw new SQLException("The table (" + this.tableDef.getName() + ") already exists");
                } else if (this.tableLoaderOptions.contains(TableLoaderOptions.DROP_TABLE)) {
                    String sql = "drop table " + this.tableDef.getName();
                    try {
                        statement.execute(sql);
                    } catch (Exception e) {
                        throw new RuntimeException("Exception with the the following sql :" + sql + "\nError Message" + e.getMessage(), e);
                    }
                    LOGGER.info("Table "+this.tableDef.getName()+" dropped");
                    Tables.create(tableDef);
                    LOGGER.info("Table "+this.tableDef.getFullyQualifiedName()+" created");

                } else if (this.tableLoaderOptions.contains(TableLoaderOptions.TRUNCATE_TABLE)) {
                    statement.execute("truncate table " + this.tableDef.getName());
                    LOGGER.info("Table "+this.tableDef.getName()+" truncated");
                }
            } else {
                if (this.tableLoaderOptions.contains(TableLoaderOptions.CREATE_TABLE_IF_NOT_EXIST) || this.tableLoaderOptions.contains(TableLoaderOptions.DROP_TABLE)) {

                    Tables.createIfNotExist(this.tableDef);

                } else {
                    throw new SQLException("The table (" + this.tableDef + ") doesn't exist and the loader doesn't have a CREATE_TABLE option, it can't then load the records.");
                }
            }
            statement.close();


            // Create the prepared statement with bind variable
            // If Merge
            List<String> statements = new ArrayList<>();
            if (this.tableLoaderOptions.contains(TableLoaderOptions.MERGE_RECORDS )) {
                if (builder.mergeColumnPositions.size()==0){
                    throw new InvalidParameterException("When choosing Merge (Upsert) has loading operations, the merge column position must be set");
                }
                String mergeStatement = database.getMergeStatement(tableDef, builder.mergeColumnPositions);
                LOGGER.info("Merge Statement: " + (mergeStatement == null ? "null" : mergeStatement));
                if (mergeStatement!=null){
                    statements.add(mergeStatement);
                } else {
                    String insertStatement = DbDml.getParameterizedInsertStatement(tableDef);
                    LOGGER.info("Insert Statement:" + insertStatement);
                    statements.add(DbDml.getParameterizedInsertStatement(tableDef));
                    String updateStatement = ""; // TODO: database.getUpdateStatement(tableDef, builder.mergeColumnPositions);;
                    LOGGER.info("Update Statement: " + (updateStatement == null ? "null" : updateStatement));
                    statements.add(updateStatement);
                }
            } else if (this.tableLoaderOptions.contains(TableLoaderOptions.INSERT_RECORDS )) {
                String insertStatement = DbDml.getParameterizedInsertStatement(tableDef);
                LOGGER.info("Insert Statement:" + insertStatement);
                statements.add(DbDml.getParameterizedInsertStatement(tableDef));

            } else if (this.tableLoaderOptions.contains(TableLoaderOptions.UPDATE_RECORDS )) {
                String updateStatement = ""; //TODO: database.getUpdateStatement(tableDef, builder.mergeColumnPositions);;
                LOGGER.info("Update Statement: " + (updateStatement == null ? "null" : updateStatement));
                statements.add(updateStatement);
            }


            // Consumer Object Instantiation
            ResultSetLoaderConsumer tableLoaderConsumer;
            //TODO: broken
//            = new ResultSetLoaderConsumer(
//                    database,
//                    statements,
//                    queue,
//                    tableDef,
//                    batchSize,
//                    producerWorkIsDone);

            // Start the threads
//            targetWorkExecutor = Executors.newFixedThreadPool(targetWorkerCount);
//            for (int i = 0; i < targetWorkerCount; i++) {
//                targetWorkExecutor.execute(tableLoaderConsumer);
//            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

    }


    /**
     * A table loader is called and must be closed when the different producers have done their jobs or not ...
     *
     * @throws Exception
     */
    @Override
    public void close() {

        producerWorkIsDone.set(true);
        queue = null;

        // Shut down the targetWorkExecutor Service
        targetWorkExecutor.shutdown();
        // And wait the termination
        try {
            targetWorkExecutor.awaitTermination(timeout, TimeUnit.MICROSECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        targetWorkExecutor = null;

    }

    /**
     * A mean to add a row by a producer
     * May be a good idea to put the timeout parameters in the process parameter
     *
     * @param objects  A row. The structure of each column must match the ColumnMetadata list given in the constructor
     * @param timeout  A timeout number
     * @param timeUnit The timeout unit (seconds, ...) from {@link TimeUnit}
     */


    /**
     * Add a row into the target buffer
     *
     * @param objects
     * @throws InterruptedException
     */
    public void addRow(Object... objects) {
        // offer method in place of the add method to avoid a java.lang.IllegalStateException: Queue full
        try {
            queue.offer(objects, timeout, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }


    public static class Builder {

        // One of the two is mandatory to be able
        // to create the table if it doesn't exist
        private Integer numberOfColumns = 1;
        private TableDef tableDef;

        // The below are not mandatory

        private Set<TableLoaderOption> tableLoaderOptions = new HashSet<>();
        private Integer targetWorkerCount = 1;
        private Long timeout = Long.MAX_VALUE;
        private Integer batchSize = 10000;
        private Database database;
        private List<Integer> mergeColumnPositions = new ArrayList<>();

        // Number of tableDef is mandatory and must be not null
        public Builder() {

        }

        public Builder numberOfColumns(Integer numberOfColumns) {
            this.numberOfColumns = numberOfColumns;
            return this;
        }

        public Builder table(TableDef tableDef) {
            this.tableDef = tableDef;
            return this;
        }

        /**
         * A Jdbc Connection Builder that contains the connection information.
         * This information will be used to create a connection for each worker (threads)
         *
         * @param database
         * @return
         */
        public Builder target(Database database) {
            this.database = database;
            return this;
        }



        /**
         * The load options given by {@link TableLoaderOptions}
         *
         * @param tableLoaderOptions
         * @return
         */
        public Builder loadOption(TableLoaderOption... tableLoaderOptions) {
            this.tableLoaderOptions = new HashSet<>(Arrays.asList(tableLoaderOptions));
            return this;
        }

        /**
         * Number of target Workers
         *
         * @param targetWorkerCount
         * @return
         */
        public Builder targetWorkerCount(Integer targetWorkerCount) {
            this.targetWorkerCount = targetWorkerCount;
            return this;
        }

        /**
         * Time Out in Micro-second
         *
         * @param timeout
         * @return
         */
        public Builder timeOut(Long timeout) {
            this.timeout = timeout;
            return this;
        }


        /**
         * Insert batch Size
         * default 10 000
         *
         * @param batchSize
         * @return
         */
        public Builder batchSize(Integer batchSize) {
            this.batchSize = batchSize;
            return this;
        }



        public TableLoader build() {
            if (this.database == null) {
                this.database = net.bytle.db.database.Databases.getSqliteDefault(); // The default one
            }
            return new TableLoader(this);
        }

        /**
         * Define the column position where the merge will apply
         * @param mergeColumnPositions
         * @return
         */
        public Builder mergeOnColumns(Integer... mergeColumnPositions) {
            this.mergeColumnPositions = Arrays.asList(mergeColumnPositions);
            return this;
        }

    }
}





