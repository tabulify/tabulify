package net.bytle.db.move;

import net.bytle.db.spi.DataPath;
import net.bytle.db.spi.Tabulars;

import java.sql.SQLException;
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
    private final DataPath sourceDataPath;
    private final DataPath targetDataPath;


    // Atomic Boolean to signal that the producer work is done
    // (Set within the close method to true)
    private AtomicBoolean producerWorkIsDone = new AtomicBoolean(false);


    // The queue (the buffer) that will be holding the data between the producer and the consumer
    BlockingQueue<Object[]> queue;

    // The thread target executor that will manage the (consumer|target) thread
    ExecutorService targetWorkExecutor;



    // The loading options
    private Set<TableLoaderOption> tableLoaderOptions = new HashSet<>();


    // Time out
    // Number of consumer thread
    private Integer targetWorkerCount = 1;
    private Long timeout = Long.MAX_VALUE;
    // Batch Size is used in the consumer side (when inserting)
    private Integer batchSize = 10000;

    private List<Integer> mergeColumnPositions = new ArrayList<>();

    TableLoader of(DataPath sourceDataPath, DataPath targetDataPath) {

        return new TableLoader(sourceDataPath, targetDataPath);
    }

    /**
     *
     */
    public TableLoader(DataPath sourceDataPath, DataPath targetDataPath) {

        this.sourceDataPath = sourceDataPath;
        this.targetDataPath = targetDataPath;




        if (tableLoaderOptions.size() == 0) {
            this.tableLoaderOptions.add(TableLoaderOptions.CREATE_TABLE_IF_NOT_EXIST);
            this.tableLoaderOptions.add(TableLoaderOptions.INSERT_RECORDS);
        } else {


            // Upsert becomes merge
            if (this.tableLoaderOptions.contains(TableLoaderOptions.UPSERT_RECORDS)) {
                this.tableLoaderOptions.remove(TableLoaderOptions.UPSERT_RECORDS);
                this.tableLoaderOptions.add(TableLoaderOptions.MERGE_RECORDS);
            }
            if (mergeColumnPositions.size() != 0 && !this.tableLoaderOptions.contains(TableLoaderOptions.UPDATE_RECORDS)) {
                this.tableLoaderOptions.add(TableLoaderOptions.MERGE_RECORDS);
            }
            // If their is no loading option, insert
            if (!(this.tableLoaderOptions.contains(TableLoaderOptions.UPSERT_RECORDS) || this.tableLoaderOptions.contains(TableLoaderOptions.INSERT_RECORDS) || this.tableLoaderOptions.contains(TableLoaderOptions.UPDATE_RECORDS))) {
                this.tableLoaderOptions.add(TableLoaderOptions.INSERT_RECORDS);
            }


        }


        // TODO: All the below need to be in the builder build method...
        // A row is just a list of objects
        Integer bufferSize = targetWorkerCount * batchSize * 2;
        queue = new ArrayBlockingQueue<>(bufferSize);

        try {
            /**
             * Not every database can make a lot of connection
             * We of the last connection object for single connection database such as sqlite.
             *
             * Example:
             *     * their is already a connection through a select for instance
             *     * and that the database does not support multiple connection (such as Sqlite)
             **/

            // One connection is already used in the construction of the database
            if (targetWorkerCount > targetDataPath.getDataSystem().getMaxWriterConnection()) {
                throw new IllegalArgumentException("The database (" + targetDataPath.getDataSystem().getProductName() + ") does not support more than (" + targetDataPath.getDataSystem().getMaxWriterConnection() + ") connections. We can then not start (" + targetWorkerCount + ") workers. (1) connection is also in use.");
            }


            /**
             * Loading preparation
             *   * Creation of the table
             *   * Creation of the statement
             */
            Boolean tableExist = Tabulars.exists(targetDataPath);

            // Go ahead if the table Exist
            if (tableExist) {
                if (this.tableLoaderOptions.contains(TableLoaderOptions.CREATE_TABLE)) {
                    throw new SQLException("The table (" + targetDataPath.toString() + ") already exists");
                } else if (this.tableLoaderOptions.contains(TableLoaderOptions.DROP_TABLE)) {

                    Tabulars.drop(targetDataPath);
                    LOGGER.info("Table " + targetDataPath.toString() + " dropped");
                    Tabulars.create(targetDataPath);
                    LOGGER.info("Table " + targetDataPath.toString() + " created");

                } else if (this.tableLoaderOptions.contains(TableLoaderOptions.TRUNCATE_TABLE)) {

                    Tabulars.truncate(targetDataPath);
                    LOGGER.info("Table " + targetDataPath.toString() + " truncated");
                }
            } else {
                if (this.tableLoaderOptions.contains(TableLoaderOptions.CREATE_TABLE_IF_NOT_EXIST) || this.tableLoaderOptions.contains(TableLoaderOptions.DROP_TABLE)) {

                    Tabulars.createIfNotExist(targetDataPath);

                } else {
                    throw new SQLException("The table (" + targetDataPath.toString() + ") doesn't exist and the loader doesn't have a CREATE_TABLE option, it can't then load the records.");
                }
            }



            // Create the prepared statement with bind variable
            // If Merge
//            List<String> statements = new ArrayList<>();
//            if (this.tableLoaderOptions.contains(TableLoaderOptions.MERGE_RECORDS)) {
//                if (mergeColumnPositions.size() == 0) {
//                    throw new InvalidParameterException("When choosing Merge (Upsert) has loading operations, the merge column position must be set");
//                }
//                String mergeStatement = database.getMergeStatement(tableDef, builder.mergeColumnPositions);
//                LOGGER.info("Merge Statement: " + (mergeStatement == null ? "null" : mergeStatement));
//                if (mergeStatement != null) {
//                    statements.add(mergeStatement);
//                } else {
//                    String insertStatement = DbDml.getParameterizedInsertStatement(tableDef);
//                    LOGGER.info("Insert Statement:" + insertStatement);
//                    statements.add(DbDml.getParameterizedInsertStatement(tableDef));
//                    String updateStatement = ""; // TODO: database.getUpdateStatement(tableDef, builder.mergeColumnPositions);;
//                    LOGGER.info("Update Statement: " + (updateStatement == null ? "null" : updateStatement));
//                    statements.add(updateStatement);
//                }
//            } else if (this.tableLoaderOptions.contains(TableLoaderOptions.INSERT_RECORDS)) {
//
//                String insertStatement = DbDml.getParameterizedInsertStatement(tableDef);
//                LOGGER.info("Insert Statement:" + insertStatement);
//                statements.add(DbDml.getParameterizedInsertStatement(tableDef));
//
//            } else if (this.tableLoaderOptions.contains(TableLoaderOptions.UPDATE_RECORDS)) {
//                String updateStatement = ""; //TODO: database.getUpdateStatement(tableDef, builder.mergeColumnPositions);;
//                LOGGER.info("Update Statement: " + (updateStatement == null ? "null" : updateStatement));
//                statements.add(updateStatement);
//            }


            // Consumer Object Instantiation
            MoveTargetWorker tableLoaderConsumer;
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


    /**
     * The load options given by {@link TableLoaderOptions}
     *
     * @param tableLoaderOptions
     * @return
     */
    public TableLoader withLoadOption(TableLoaderOption... tableLoaderOptions) {
        this.tableLoaderOptions = new HashSet<>(Arrays.asList(tableLoaderOptions));
        return this;
    }

    /**
     * Number of target Workers
     *
     * @param targetWorkerCount
     * @return
     */
    public TableLoader withTargetWorkerCount(Integer targetWorkerCount) {
        this.targetWorkerCount = targetWorkerCount;
        return this;
    }

    /**
     * Time Out in Micro-second
     *
     * @param timeout
     * @return
     */
    public TableLoader withTimeOut(Long timeout) {
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
    public TableLoader withBatchSize(Integer batchSize) {
        this.batchSize = batchSize;
        return this;
    }


    /**
     * Define the column position where the merge will apply
     *
     * @param mergeColumnPositions
     * @return
     */
    public TableLoader withMergeOnColumns(Integer... mergeColumnPositions) {
        this.mergeColumnPositions = Arrays.asList(mergeColumnPositions);
        return this;
    }

}





