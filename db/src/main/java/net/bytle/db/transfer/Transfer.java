package net.bytle.db.transfer;


import net.bytle.db.memory.MemoryDataPath;
import net.bytle.db.spi.DataPath;
import net.bytle.db.stream.InsertStreamListener;
import net.bytle.log.Log;

import java.sql.Types;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;


/**
 * A class to move a data document
 */
public class Transfer {

    public static final Log LOGGER = Log.getLog(Transfer.class);

    private final List<Integer> typesNotSupported = Arrays.asList(
            Types.ARRAY,
            Types.BINARY,
            Types.BLOB,
            Types.CLOB,
            Types.BIT
    );

    public static List<MoveListener> load(DataPath sourceDef, DataPath targetDataPath, TransferProperties transferProperties) {


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
        int targetWorkerCount = transferProperties.getTargetWorkerCount();
        if (targetWorkerCount > targetDataPath.getDataSystem().getMaxWriterConnection()) {
            throw new IllegalArgumentException("The database (" + targetDataPath.getDataSystem().getProductName() + ") does not support more than (" + targetDataPath.getDataSystem().getMaxWriterConnection() + ") connections. We can then not start (" + targetWorkerCount + ") workers. (1) connection is also in use.");
        }



        // The dead objects - They will be inserted in the queue at the end to send a termination message
        AtomicBoolean producerWorkIsDone = new AtomicBoolean(false);
        AtomicBoolean consumerWorkIsDone = new AtomicBoolean(false);

        long timeout = transferProperties.getTimeOut();

        // The queue between the producer (source) and the consumer (target)
        MemoryDataPath queue = MemoryDataPath.of("Transfer")
                .setBlocking(true)
                .setTimeout(timeout)
                .setCapacity(transferProperties.getQueueSize());


        try {


            TransferSourceWorker transferSourceWorker = new TransferSourceWorker(sourceDef, queue, streamListeners, transferProperties);
            Thread producer = new Thread(transferSourceWorker);
            producer.start();


            // Start the target threads
            ExecutorService targetWorkExecutor = Executors.newFixedThreadPool(targetWorkerCount);
            for (int i = 0; i < targetWorkerCount; i++) {

                targetWorkExecutor.execute(
                        new MoveTargetWorker(
                                targetDataPath,
                                sourceDef,
                                queue,
                                transferProperties,
                                producerWorkIsDone,
                                streamListeners)
                );

            }

            TransferMetricsViewer TRansferMetricsViewer = new TransferMetricsViewer(queue, transferProperties, streamListeners, producerWorkIsDone, consumerWorkIsDone);
            Thread viewer = new Thread(TRansferMetricsViewer);
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





