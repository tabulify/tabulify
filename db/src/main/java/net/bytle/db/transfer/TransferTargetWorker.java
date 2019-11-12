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
    private final TransferProperties transferProperties;

    public TransferTargetWorker(
            DataPath sourceDataPath,
            DataPath targetDataPath,
            TransferProperties transferProperties,
            AtomicBoolean producerWorkIsDone) {
        this.queue = sourceDataPath;
        this.targetDataPath = targetDataPath;
        this.producerWorkIsDone = producerWorkIsDone;
        this.transferProperties = transferProperties;

    }

    @Override
    public void run() {
        String name = "Consumer: " + Thread.currentThread().getName();
        TransferListener transferListener = TransferListener.of();
        try (

            InsertStream insertStream = Tabulars.getInsertStream(targetDataPath)
                    .setName(name)
                    .setCommitFrequency(transferProperties.getCommitFrequency())
                    .setBatchSize(transferProperties.getBatchSize());

            SelectStream selectStream = Tabulars.getSelectStream(queue)
                    .setName(name);

        ){

            transferListener.addInsertListener(insertStream.getInsertStreamListener());
            transferListener.addSelectListener(selectStream.getSelectStreamListener());

            List<Object> objects;
            while (true) {
                while (selectStream.next(transferProperties.getTimeOut(), TimeUnit.SECONDS)) {

                    objects = selectStream.getObjects();
                    insertStream.insert(objects);

                }
                if (producerWorkIsDone.get()) {
                    break;
                }
            }

        } catch (Exception e) {
            transferListener.addException(e);
            throw new RuntimeException(e);
        }

    }
}
