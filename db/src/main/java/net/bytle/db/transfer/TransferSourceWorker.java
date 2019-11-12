package net.bytle.db.transfer;


import net.bytle.db.spi.DataPath;
import net.bytle.db.spi.Tabulars;
import net.bytle.db.stream.*;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * A worker that takes data from the source and insert them into the memory queue
 */
public class TransferSourceWorker implements Runnable {


    private final DataPath sourceDataPath;
    private final DataPath queue;
    private final Integer feedbackFrequency;
    private final TransferProperties transferProperties;


    /**
     * @param sourceDataPath
     * @param queue          (A blocking queue !)
     */
    public TransferSourceWorker(DataPath sourceDataPath, DataPath queue, TransferProperties transferProperties) {

        this.sourceDataPath = sourceDataPath;
        this.queue = queue;
        this.feedbackFrequency = transferProperties.getFeedbackFrequency();
        this.transferProperties = transferProperties;

    }


    @Override
    public void run() {

        TransferListener transferListener = TransferListener.of();
        try (
                InsertStream insertStream = Tabulars.getInsertStream(queue)
                        .setName("Producer: " + Thread.currentThread().getName())
                        .setFeedbackFrequency(feedbackFrequency);
        ) {

            transferListener.addInsertListener(insertStream.getInsertStreamListener());

            SelectStream selectStream = Tabulars.getSelectStream(sourceDataPath);
            List<Object> objects;
            int columnCount = sourceDataPath.getDataDef().getColumnDefs().size();
            while (selectStream.next()) {

                objects = IntStream.of(columnCount)
                        .mapToObj(selectStream::getObject)
                        .collect(Collectors.toList());
                insertStream.insert(objects);

            }

        } catch (Exception e) {

            transferListener.addException(e);
            throw new RuntimeException(e);

        }

    }
}

