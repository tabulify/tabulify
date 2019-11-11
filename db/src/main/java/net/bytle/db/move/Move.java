package net.bytle.db.move;


import net.bytle.db.spi.DataPath;
import net.bytle.db.stream.InsertStreamListener;
import net.bytle.db.stream.SelectStreamListener;

import java.sql.Types;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;


/**
 *
 * A class to move a data document
 *
 */
public class Move {



    private final List<Integer> typesNotSupported = Arrays.asList(
            Types.ARRAY,
            Types.BINARY,
            Types.BLOB,
            Types.CLOB,
            Types.BIT
    );

    public static List<MoveListener> load(DataPath sourceDef, DataPath targetTableDef, MoveProperties moveProperties) {


        List<InsertStreamListener> streamListeners = Collections.synchronizedList(new ArrayList<>());
        List<SelectStreamListener> selectStreamListeners = Collections.synchronizedList(new ArrayList<>());

        try {

            // Thread can start below
            AtomicBoolean producerWorkIsDone = new AtomicBoolean(false);
            AtomicBoolean consumerWorkIsDone = new AtomicBoolean(false);


            //TODO
            // BlockingQueue<List<Object>> queue = new ArrayBlockingQueue<>(bufferSize);
            DataPath queue = null;
            // The listener to be able to see when exceptions occurs in the thread

            MoveSourceWorker moveSourceWorker = new MoveSourceWorker(sourceDef, queue, selectStreamListeners,moveProperties);
            Thread producer = new Thread(moveSourceWorker);
            producer.start();


            // Start the threads
            int targetWorkerCount = moveProperties.getTargetWorkerCount();
            ExecutorService targetWorkExecutor = Executors.newFixedThreadPool(targetWorkerCount);
            for (int i = 0; i < targetWorkerCount; i++) {

                targetWorkExecutor.execute(
                        new MoveTargetWorker(
                                targetTableDef,
                                sourceDef,
                                queue,
                                moveProperties,
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




}





