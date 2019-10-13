package net.bytle.db.loader;

import net.bytle.db.model.RelationDef;
import net.bytle.db.model.TableDef;
import net.bytle.db.stream.InsertStream;
import net.bytle.db.stream.InsertStreamListener;
import net.bytle.db.stream.SqlInsertStream;
import net.bytle.cli.Log;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by gerard on 29-01-2016.
 */
public class ResultSetLoaderConsumer implements Runnable {

    private static final Log LOGGER = Loaders.LOGGER_DB_LOADER;

    private final BlockingQueue<List<Object>> queue;
    private final AtomicBoolean producerWorkIsDone;
    private final Integer batchSize;
    private final TableDef tableDef;
    private final Integer commitFrequency;
    private final List<InsertStreamListener> listeners;
    private final RelationDef sourceDef;

    public ResultSetLoaderConsumer(
            TableDef tableDef,
            RelationDef source,
            BlockingQueue<List<Object>> queue,
            Integer batchSize,
            Integer commitFrequency,
            AtomicBoolean producerWorkIsDone,
            List<InsertStreamListener> listeners)
    {
        this.tableDef = tableDef;
        this.sourceDef = source;
        this.queue = queue;
        this.producerWorkIsDone = producerWorkIsDone;
        this.batchSize = batchSize;
        this.commitFrequency = commitFrequency;
        this.listeners = listeners;

    }

    @Override
    public void run() {


        InsertStream insertStream = SqlInsertStream.get(tableDef)
                .setDataDef(sourceDef)
                .setName("Consumer: " + Thread.currentThread().getName())
                .setCommitFrequency(this.commitFrequency)
                .setBatchSize(batchSize);

        InsertStreamListener listener = insertStream.getInsertStreamListener();
        // Collect the listener
        this.listeners.add(listener);

        try {


            List<Object> objects;
            while (!(queue.isEmpty() && producerWorkIsDone.get())) {

                // The poll method to prevent waiting indefinitely
                // if the queue is empty and that the producerWorkIsDone
                // with the take method.
                objects = queue.poll(1, TimeUnit.SECONDS);
                if (objects == null) {
                    LOGGER.warning("The timeout to of a element in the queue was reached."
                            + " As it may be the end of the queue, we continue.)");
                    continue;
                }
                insertStream.insert(objects);

            }
            insertStream.close();

        } catch (Exception e) {
            // Bad but yeah, it should be a thread listener
            listener.addException(e);
            throw new RuntimeException(e);
        }

    }
}
