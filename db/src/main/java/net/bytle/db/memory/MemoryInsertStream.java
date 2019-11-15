package net.bytle.db.memory;

import net.bytle.db.transfer.TransferLog;
import net.bytle.db.stream.InsertStream;
import net.bytle.db.stream.InsertStreamAbs;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

public class MemoryInsertStream extends InsertStreamAbs implements InsertStream {


    private final MemoryDataPath memoryDataPath;

    private Collection<? extends List> tabular;

    private int currentRowInBatch = 0;
    private int batchExecutionCount = 0;

    public MemoryInsertStream(MemoryDataPath memoryDataPath) {
        super(memoryDataPath);
        this.memoryDataPath = memoryDataPath;

        final MemoryStore memoryStore = memoryDataPath.getDataSystem().getMemoryStore();
        tabular = memoryStore.getValues(memoryDataPath);
        if (tabular==null){
            switch (memoryDataPath.getType()) {
                case TYPE_BLOCKED_QUEUE:
                    this.tabular = new ArrayBlockingQueue<>(memoryDataPath.getCapacity());
                    break;
                case TYPE_LIST:
                    this.tabular = new ArrayList<>();
                    break;
                default:
                    throw new RuntimeException("Type ("+memoryDataPath.getType()+") is unknown for this memory data path ("+memoryDataPath+")");
            }
            memoryStore.put(memoryDataPath, tabular);
        }
    }


    @Override
    public InsertStream insert(List<Object> objects) {

        currentRowInBatch++;
        // Offer method in place of the add method to avoid a java.lang.IllegalStateException: Queue full
        int timeout = memoryDataPath.getTimeOut();
        boolean result;
        try {
            switch (memoryDataPath.getType()) {
                case TYPE_BLOCKED_QUEUE:
                    result = ((ArrayBlockingQueue) tabular).offer(objects, timeout, TimeUnit.SECONDS);
                    break;
                case TYPE_LIST:
                    result = ((ArrayList) tabular).add(objects);
                    break;
                default:
                    throw new RuntimeException("Type ("+memoryDataPath.getType()+") is unknown for this memory data path ("+memoryDataPath+")");
            }

        } catch (InterruptedException e) {
            this.insertStreamListener.addException(e);
            throw new RuntimeException(e);
        }

        if (!result) {
            final String msg = name + ": the timeout of (" + timeout + ") seconds was reached (unable to add rows to the queue, stopping the data load).";
            TransferLog.LOGGER.severe(msg);
            throw new RuntimeException(msg);
        }

        // Batch processing
        // because the log feedback is based on it
        if (currentRowInBatch >= this.batchSize) {
            process_batch_info();
        }

        return this;
    }

    private void process_batch_info() {
        insertStreamListener.addRows(currentRowInBatch);
        insertStreamListener.incrementBatch();
        currentRowInBatch = 0;
        batchExecutionCount++;
        if (Math.floorMod(batchExecutionCount, feedbackFrequency) == 0) {
            TransferLog.LOGGER.info(name + ": " + insertStreamListener.getRowCount() + " rows loaded in "+this.dataPath.toString());
        }
    }

    @Override
    public void close() {
        process_batch_info();
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
