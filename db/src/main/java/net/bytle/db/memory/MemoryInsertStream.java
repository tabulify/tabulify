package net.bytle.db.memory;

import net.bytle.log.Log;
import net.bytle.db.DbLoggers;
import net.bytle.db.stream.InsertStream;
import net.bytle.db.stream.InsertStreamAbs;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

public class MemoryInsertStream extends InsertStreamAbs implements InsertStream {


    private static final Log LOGGER = DbLoggers.LOGGER_DB_ENGINE;
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
                case MemoryDataPath.TYPE_BLOCKED_QUEUE:
                    this.tabular = new ArrayBlockingQueue<>(memoryDataPath.getCapacity());
                    break;
                case MemoryDataPath.TYPE_LIST:
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
        int timeout = 60;
        boolean result;
        try {
            switch (memoryDataPath.getType()) {
                case MemoryDataPath.TYPE_BLOCKED_QUEUE:
                    result = ((ArrayBlockingQueue) tabular).offer(objects, timeout, TimeUnit.SECONDS);
                    break;
                case MemoryDataPath.TYPE_LIST:
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
                LOGGER.info(name + ": " + insertStreamListener.getRowCount() + " rows loaded in "+this.dataPath.toString());
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
