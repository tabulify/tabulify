package net.bytle.db.transfer;

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
    private final TransferSourceTarget transferSourceTarget;
    private final TransferProperties transferProperties;

    public TransferTargetWorker(
            DataPath queue,
            TransferSourceTarget transferSourceTarget,
            TransferProperties transferProperties,
            AtomicBoolean producerWorkIsDone) {
        this.queue = queue;
        this.transferSourceTarget = transferSourceTarget;
        this.producerWorkIsDone = producerWorkIsDone;
        this.transferProperties = transferProperties;

    }

    @Override
    public void run() {
        String name = "Consumer: " + Thread.currentThread().getName();
        TransferListenerStream transferListenerStream = new TransferListenerStream(transferSourceTarget);
        try (

            InsertStream insertStream = Tabulars.getInsertStream(transferSourceTarget.getTargetDataPath())
                    .setName(name)
                    .setCommitFrequency(transferProperties.getCommitFrequency())
                    .setBatchSize(transferProperties.getBatchSize());

            SelectStream selectStream = Tabulars.getSelectStream(queue)
                    .setName(name);

        ){

            transferListenerStream.addInsertListener(insertStream.getInsertStreamListener());
            transferListenerStream.addSelectListener(selectStream.getSelectStreamListener());

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
            transferListenerStream.addException(e);
            throw new RuntimeException(e);
        }

    }
}
