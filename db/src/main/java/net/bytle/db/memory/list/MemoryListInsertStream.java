package net.bytle.db.memory.list;

import net.bytle.db.stream.InsertStream;
import net.bytle.db.stream.InsertStreamAbs;
import net.bytle.db.transfer.TransferLog;

import java.util.List;

public class MemoryListInsertStream extends InsertStreamAbs implements InsertStream {


  private final MemoryListDataPath memoryListDataPath;

  private List<List<Object>> tabular;

  private int currentRowInBatch = 0;
  private int batchExecutionCount = 0;

  public MemoryListInsertStream(MemoryListDataPath memoryListDataPath) {
    super(memoryListDataPath);
    this.memoryListDataPath = memoryListDataPath;
    this.tabular = memoryListDataPath.getValues();
    if (tabular == null){
      throw  new RuntimeException("The memory data path ("+memoryListDataPath+") does not exist (or was not created before insertion)");
    }
  }


  @Override
  public InsertStream insert(List<Object> objects) {

    currentRowInBatch++;
    tabular.add(objects);

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

  /**
   * In case of parent child hierarchy
   * we can check if we need to send the data with the function nextInsertSendBatch()
   * and send it with this function
   */
  @Override
  public void flush() {

  }


}
