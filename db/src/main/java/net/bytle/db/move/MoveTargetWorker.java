package net.bytle.db.move;

import net.bytle.db.stream.InsertStreamListener;
import net.bytle.log.Log;
import net.bytle.db.DbLoggers;
import net.bytle.db.spi.DataPath;
import net.bytle.db.spi.Tabulars;
import net.bytle.db.stream.InsertStream;
import net.bytle.db.stream.SelectStream;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by gerard on 29-01-2016.
 */
public class MoveTargetWorker implements Runnable {

    private static final Log LOGGER = DbLoggers.LOGGER_DB_ENGINE;

    private final DataPath queue;
    private final AtomicBoolean producerWorkIsDone;
    private final Integer batchSize;
    private final DataPath tableDef;
    private final Integer commitFrequency;
    private final List<InsertStreamListener> listeners;
    private final DataPath sourceDef;

    public MoveTargetWorker(
            DataPath targetDataPath,
            DataPath sourceDataPath,
            DataPath intermediate,
            MoveProperties moveProperties,
            AtomicBoolean producerWorkIsDone,
            List<InsertStreamListener> listeners)
    {
        this.tableDef = targetDataPath;
        this.sourceDef = sourceDataPath;
        this.queue = intermediate;
        this.producerWorkIsDone = producerWorkIsDone;
        this.batchSize = moveProperties.getBatchSize();
        this.commitFrequency = moveProperties.getCommitFrequency();
        this.listeners = listeners;

    }

    @Override
    public void run() {


        InsertStream insertStream = Tabulars.getInsertStream(tableDef)
                .setName("Consumer: " + Thread.currentThread().getName())
                .setCommitFrequency(this.commitFrequency)
                .setBatchSize(batchSize);

        InsertStreamListener listener = insertStream.getInsertStreamListener();
        // Collect the listener
        this.listeners.add(listener);
        SelectStream selectStream = Tabulars.getSelectStream(queue);
        try {


            List<Object> objects;
            while (!(Tabulars.isEmpty(queue) && producerWorkIsDone.get())) {

                // The poll method to prevent waiting indefinitely
                // if the queue is empty and that the producerWorkIsDone
                // with the take method.
                objects = selectStream.poll(1, TimeUnit.SECONDS);
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
