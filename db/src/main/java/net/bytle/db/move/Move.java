package net.bytle.db.move;


import net.bytle.db.DbLoggers;
import net.bytle.db.spi.DataPath;
import net.bytle.db.spi.Tabulars;
import net.bytle.db.stream.InsertStreamListener;
import net.bytle.db.stream.SelectStreamListener;
import net.bytle.log.Log;

import java.sql.SQLException;
import java.sql.Types;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;


/**
 * A class to move a data document
 */
public class Move {

    public static final Log LOGGER = Log.getLog(Move.class);

    private final List<Integer> typesNotSupported = Arrays.asList(
            Types.ARRAY,
            Types.BINARY,
            Types.BLOB,
            Types.CLOB,
            Types.BIT
    );

    public static List<MoveListener> load(DataPath sourceDef, DataPath targetDataPath, MoveProperties moveProperties) {


        List<InsertStreamListener> streamListeners = Collections.synchronizedList(new ArrayList<>());


        /**
         * Not every database can make a lot of connection
         * We of the last connection object for single connection database such as sqlite.
         *
         * Example:
         *     * their is already a connection through a select for instance
         *     * and that the database does not support multiple connection (such as Sqlite)
         **/

        // One connection is already used in the construction of the database
        int targetWorkerCount = moveProperties.getTargetWorkerCount();
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

            Tabulars.drop(targetDataPath);
            LOGGER.info("Table " + targetDataPath.toString() + " dropped");
            Tabulars.create(targetDataPath);
            LOGGER.info("Table " + targetDataPath.toString() + " created");

        }

        // Thread can start below
        AtomicBoolean producerWorkIsDone = new AtomicBoolean(false);
        AtomicBoolean consumerWorkIsDone = new AtomicBoolean(false);

        //TODO
        // BlockingQueue<List<Object>> queue = new ArrayBlockingQueue<>(bufferSize);
        DataPath queue = null;
        // The listener to be able to see when exceptions occurs in the thread

        long timeout = moveProperties.getTimeOut();
        try {


            MoveSourceWorker moveSourceWorker = new MoveSourceWorker(sourceDef, queue, streamListeners, moveProperties);
            Thread producer = new Thread(moveSourceWorker);
            producer.start();


            // Start the threads
            ExecutorService targetWorkExecutor = Executors.newFixedThreadPool(targetWorkerCount);
            for (int i = 0; i < targetWorkerCount; i++) {

                targetWorkExecutor.execute(
                        new MoveTargetWorker(
                                targetDataPath,
                                sourceDef,
                                queue,
                                moveProperties,
                                producerWorkIsDone,
                                streamListeners)
                );

            }

            MoveMetricsViewer moveMetricsViewer = new MoveMetricsViewer(queue, moveProperties, streamListeners, producerWorkIsDone, consumerWorkIsDone);
            Thread viewer = new Thread(moveMetricsViewer);
            viewer.start();

            // Wait the producer
            producer.join(); // Waits for this thread to die.
            producerWorkIsDone.set(true);

            // Shut down the targetWorkExecutor Service
            targetWorkExecutor.shutdown();
            // And wait the termination
            try {
                Boolean result = targetWorkExecutor.awaitTermination(timeout, TimeUnit.MICROSECONDS);
                if (!result) {
                    throw new RuntimeException("The timeout of the consumers (" + timeout + " ms) elapsed before termination");
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            consumerWorkIsDone.set(true);

            // Wait the viewer
            viewer.join();


        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            // Close things here if something is going wrong
            producerWorkIsDone.set(true);

        }

        return null;

    }

}





