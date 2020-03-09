package net.bytle.db.transfer;


import net.bytle.db.spi.DataPath;
import net.bytle.db.spi.Tabulars;
import net.bytle.db.stream.*;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * A worker that takes data from the source and insert them into the memory queue
 */
public class TransferSourceWorker implements Runnable {


    private final DataPath sourceDataPath;
    private final DataPath queue;
    private final Integer feedbackFrequency;
    private final TransferListenerStream transferListenerStream;


    /**
     * @param sourceDataPath
     * @param queue          (The target data path, a blocking queue !)
     * @param transferProperties - The properties of the transfer
     * @param transferListenerStream - The cross thread listeners used in the viewer thread
     */
    public TransferSourceWorker(DataPath sourceDataPath, DataPath queue, TransferProperties transferProperties, TransferListenerStream transferListenerStream) {

        this.sourceDataPath = sourceDataPath;
        this.queue = queue;
        this.feedbackFrequency = transferProperties.getFeedbackFrequency();
        this.transferListenerStream = transferListenerStream;

    }


    @Override
    public void run() {

        String name = "Producer: " + Thread.currentThread().getName();
        try (

                SelectStream selectStream = Tabulars.getSelectStream(sourceDataPath)
                        .setName(name);

                InsertStream insertStream = Tabulars.getInsertStream(queue)
                        .setName(name)
                        .setFeedbackFrequency(feedbackFrequency);

        ) {

            // The feedback
            transferListenerStream.addSelectListener(selectStream.getSelectStreamListener());
            InsertStreamListener insertStreamListener = insertStream.getInsertStreamListener();
            transferListenerStream.addInsertListener(insertStreamListener);

            // The transfer
            int columnCount = sourceDataPath.getOrCreateDataDef().getColumnsSize();
            while (selectStream.next()) {

                List<Object> objects = IntStream.of(columnCount)
                        .mapToObj(selectStream::getObject)
                        .collect(Collectors.toList());
                insertStream.insert(objects);

            }

        } catch (Exception e) {

            transferListenerStream.addException(e);
            throw new RuntimeException(e);

        }

    }
}

