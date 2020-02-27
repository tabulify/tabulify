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
    private final TransferListener transferListener;


    /**
     * @param sourceDataPath
     * @param queue          (The target data path, a blocking queue !)
     * @param transferProperties - The properties of the transfer
     * @param transferListener - The cross thread listeners used in the viewer thread
     */
    public TransferSourceWorker(DataPath sourceDataPath, DataPath queue, TransferProperties transferProperties, TransferListener transferListener) {

        this.sourceDataPath = sourceDataPath;
        this.queue = queue;
        this.feedbackFrequency = transferProperties.getFeedbackFrequency();
        this.transferListener = transferListener;

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
            transferListener.addSelectListener(selectStream.getSelectStreamListener());
            InsertStreamListener insertStreamListener = insertStream.getInsertStreamListener();
            transferListener.addInsertListener(insertStreamListener);

            // The transfer
            int columnCount = sourceDataPath.getOrCreateDataDef().getColumnsSize();
            while (selectStream.next()) {

                List<Object> objects = IntStream.of(columnCount)
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

