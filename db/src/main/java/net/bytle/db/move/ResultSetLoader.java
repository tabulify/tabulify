package net.bytle.db.move;


import net.bytle.db.spi.DataPath;
import net.bytle.db.stream.MoveListener;

import java.sql.Types;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;


/**
 * Created by gerard on 9/17/2015.
 * A class to load a data set in a targetDatabase
 * through JDBC
 */
public class ResultSetLoader {




    private final List<Integer> typesNotSupported = new ArrayList(Arrays.asList(
            Types.ARRAY,
            Types.BINARY,
            Types.BLOB,
            Types.CLOB,
            Types.BIT
    ));
    private final DataPath sourceDef;
    public Properties tableAttributes = new Properties();
    private int batchSize = 10000;


    private Integer targetWorkerCount = 1;
    private long timeout = Long.MAX_VALUE;
    private Integer bufferSize;
    private Integer commitFrequency = 10000;

    private String metricsFilePath;
    private DataPath targetTableDef;


    /**
     * @param targetTableDef
     * @param source The data source implementing
     */
    public ResultSetLoader(DataPath source, DataPath targetTableDef) {
        this.targetTableDef = targetTableDef;
        this.sourceDef = source;
    }


    /**
     * Number of target Workers
     *
     * @param targetWorkerCount
     * @return
     */
    public ResultSetLoader targetWorkerCount(Integer targetWorkerCount) {
        this.targetWorkerCount = targetWorkerCount;
        return this;
    }

    /**
     * Time Out in Micro-second
     *
     * @param timeout
     * @return
     */
    public ResultSetLoader timeOut(long timeout) {
        this.timeout = timeout;
        return this;
    }


    /**
     * Number of rows fetched with each database round trip for a query,
     * 10 for Oracle row-prefetch value.
     * http://docs.oracle.com/cd/B19306_01/java.102/b14355/resltset.htm#i1023619
     * Changes made to the fetch size of a statement object after a result set is produced will have
     * no affect on that result set.
     *
     * @param bufferSize
     * @return
     */
    public ResultSetLoader bufferSize(Integer bufferSize) {
        this.bufferSize = bufferSize;
        return this;
    }

    /**
     * Insert batch Size
     * default 10 000
     *
     * @param batchSize
     * @return
     */
    public ResultSetLoader batchSize(Integer batchSize) {
        this.batchSize = batchSize;
        return this;
    }

    /**
     * Commit Frequency by batch size
     * default 10000
     *
     * @param commitFrequency
     * @return
     */
    public ResultSetLoader commitFrequency(Integer commitFrequency) {
        this.commitFrequency = commitFrequency;
        return this;
    }

    public List<MoveListener> load() {


        List<MoveListener> streamListeners = Collections.synchronizedList(new ArrayList<>());

        try {

            // Thread can start below
            AtomicBoolean producerWorkIsDone = new AtomicBoolean(false);
            AtomicBoolean consumerWorkIsDone = new AtomicBoolean(false);

            // A row is just a list of objects
            if (bufferSize == null) {
                bufferSize = targetWorkerCount * batchSize * 2;
            }

            //TODO
            // BlockingQueue<List<Object>> queue = new ArrayBlockingQueue<>(bufferSize);
            DataPath queue = null;
            // The listener to be able to see when exceptions occurs in the thread

            ResultSetLoaderProducer resultSetLoaderProducer = new ResultSetLoaderProducer(sourceDef, queue, streamListeners, 100000);
            Thread producer = new Thread(resultSetLoaderProducer);
            producer.start();


            // Start the threads
            ExecutorService targetWorkExecutor = Executors.newFixedThreadPool(targetWorkerCount);
            for (int i = 0; i < targetWorkerCount; i++) {

                targetWorkExecutor.execute(
                        new ResultSetLoaderConsumer(
                                targetTableDef,
                                sourceDef,
                                queue,
                                batchSize,
                                commitFrequency,
                                producerWorkIsDone,
                                streamListeners)
                );

            }

            ResultSetLoaderMetricsViewer resultSetLoaderMetricsViewer = new ResultSetLoaderMetricsViewer(queue, bufferSize, streamListeners, metricsFilePath, producerWorkIsDone, consumerWorkIsDone);
            Thread viewer = new Thread(resultSetLoaderMetricsViewer);
            viewer.start();

            // Wait the producer
            producer.join(); // Waits for this thread to die.
            producerWorkIsDone.set(true);

            // Shut down the targetWorkExecutor
            targetWorkExecutor.shutdown();
            // And wait the termination
            Boolean result = targetWorkExecutor.awaitTermination(timeout, TimeUnit.MICROSECONDS);
            if (!result) {
                throw new RuntimeException("The timeout of the consumers (" + timeout + " ms) elapsed before termination");
            }
            consumerWorkIsDone.set(true);

            // Wait the viewer
            viewer.join();


        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return streamListeners;
    }


    public ResultSetLoader addTableAttribute(String key, String value) {
        this.tableAttributes.put(key, value);
        return this;
    }

    /**
     * Where to output the metrics (default to system.out if null)
     *
     * @param metricsFilePath
     * @return
     */
    public ResultSetLoader metricsFilePath(String metricsFilePath) {
        this.metricsFilePath = metricsFilePath;
        return this;
    }
}





