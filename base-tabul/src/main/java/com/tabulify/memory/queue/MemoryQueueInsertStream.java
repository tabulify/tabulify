package com.tabulify.memory.queue;

import com.tabulify.stream.InsertStream;
import com.tabulify.stream.InsertStreamAbs;
import com.tabulify.transfer.TransferLog;

import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

public class MemoryQueueInsertStream extends InsertStreamAbs implements InsertStream {


  private final MemoryQueueDataPath memoryQueueDataPath;

  private final ArrayBlockingQueue<List<Object>> listObjectQueue;

  private int currentRowInBatch = 0;
  private int batchExecutionCount = 0;

  public MemoryQueueInsertStream(MemoryQueueDataPath memoryQueueDataPath) {
    super(memoryQueueDataPath);
    this.memoryQueueDataPath = memoryQueueDataPath;
    this.listObjectQueue = memoryQueueDataPath.getValues();
  }


  @Override
  public InsertStream insert(List<Object> objects) {

    currentRowInBatch++;
    // Offer method in place of the add method to avoid a java.lang.IllegalStateException: Queue full
    int timeout = memoryQueueDataPath.getTimeOut();
    boolean result;
    try {
      result = listObjectQueue.offer(objects, timeout, TimeUnit.SECONDS);
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
      TransferLog.LOGGER.info(name + ": " + insertStreamListener.getRowCount() + " rows loaded in " + this.dataPath.toString());
    }
  }

  @Override
  public void close() {
    process_batch_info();
  }


  @Override
  public void flush() {

  }


}
