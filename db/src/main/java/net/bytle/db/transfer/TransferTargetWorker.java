package net.bytle.db.transfer;

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
 *
 */
public class TransferTargetWorker implements Runnable {


    private final DataPath queue;
    private final AtomicBoolean producerWorkIsDone;
    private final DataPath targetDataPath;
    private final List<InsertStreamListener> listeners;
    private final TransferProperties transferProperties;

    public TransferTargetWorker(
            DataPath sourceDataPath,
            DataPath targetDataPath,
            TransferProperties transferProperties,
            AtomicBoolean producerWorkIsDone,
            List<InsertStreamListener> listeners) {
        this.queue = sourceDataPath;
        this.targetDataPath = targetDataPath;
        this.producerWorkIsDone = producerWorkIsDone;
        this.transferProperties = transferProperties;
        this.listeners = listeners;

    }

    @Override
    public void run() {


        InsertStream insertStream = Tabulars.getInsertStream(targetDataPath)
                .setName("Consumer: " + Thread.currentThread().getName())
                .setCommitFrequency(transferProperties.getCommitFrequency())
                .setBatchSize(transferProperties.getBatchSize());

        InsertStreamListener listener = insertStream.getInsertStreamListener();
        this.listeners.add(listener);

        SelectStream selectStream = Tabulars.getSelectStream(queue);
        try {

            List<Object> objects;
            while (
                    producerWorkIsDone.get()
            ) {

                while (selectStream.next(transferProperties.getTimeOut(), TimeUnit.SECONDS)) {

                    objects = selectStream.getObjects();
                    insertStream.insert(objects);

                }

            }
            insertStream.close();

        } catch (Exception e) {
            listener.addException(e);
            throw new RuntimeException(e);
        }

    }
}
