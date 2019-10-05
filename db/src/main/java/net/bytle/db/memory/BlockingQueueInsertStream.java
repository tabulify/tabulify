package net.bytle.db.memory;

import net.bytle.db.DbLoggers;
import net.bytle.cli.Log;
import net.bytle.db.stream.InsertStream;
import net.bytle.db.stream.InsertStreamAbs;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

public class BlockingQueueInsertStream extends InsertStreamAbs implements InsertStream {


    private static final Log LOGGER = DbLoggers.LOGGER_DB_ENGINE;

    private final BlockingQueue<List<Object>> queue;
    private int currentRowInBatch = 0;
    private int batchExecutionCount = 0;

    public BlockingQueueInsertStream(BlockingQueue<List<Object>> queue) {
        this.queue = queue;
    }

    public static InsertStream get(BlockingQueue<List<Object>> queue) {
        return new BlockingQueueInsertStream(queue);
    }

    @Override
    public InsertStream insert(List<Object> objects) {

        currentRowInBatch++;
        // Offer method in place of the add method to avoid a java.lang.IllegalStateException: Queue full
        int timeout = 60;
        boolean result;
        try {
            result = queue.offer(objects, timeout, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            this.insertStreamListener.addException(e);
            throw new RuntimeException(e);
        }

        if (!result) {
            final String msg = name + ": the timeout of (" + timeout + ") seconds was reached (unable to add rows to the queue, stopping the data load).";
            LOGGER.severe(msg);
            throw new RuntimeException(msg);
        }

        // Batch processing
        // because the log feedback is based on it
        if (currentRowInBatch >= this.batchSize) {
            insertStreamListener.addRows(currentRowInBatch);
            currentRowInBatch = 0;
            batchExecutionCount++;
            if (Math.floorMod(batchExecutionCount, feedbackFrequency) == 0) {
                LOGGER.info(name + ": " + insertStreamListener.getRowCount() + " rows loaded in queue ");
            }
        }

        return this;
    }

    @Override
    public void close() {

    }

    /**
     * In case of parent child hierarchy
     * we can check if we need to send the data with the function nextInsertSendBatch()
     * and send it with this function
     */
    @Override
    public void flush() {

    }


}
