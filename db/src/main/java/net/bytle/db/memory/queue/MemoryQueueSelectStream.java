package net.bytle.db.memory.queue;

import net.bytle.db.memory.MemoryDataPathType;
import net.bytle.db.model.RelationDef;
import net.bytle.db.stream.SelectStream;
import net.bytle.db.stream.SelectStreamAbs;

import java.sql.Clob;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Select implementation
 */
public class MemoryQueueSelectStream extends SelectStreamAbs implements SelectStream {

  private ArrayBlockingQueue<List<Object>> tabular;
  private final MemoryQueueDataPath memoryQueueDataPath;

  // Index is used for a list for a queue
  private int rowIndex = -1;
  private List<Object> currentRow;

  MemoryQueueSelectStream(MemoryQueueDataPath memoryQueueDataPath) {
    super(memoryQueueDataPath);
    this.memoryQueueDataPath = memoryQueueDataPath;
    this.tabular = memoryQueueDataPath.getValues();
  }


  @Override
  public boolean next() {

    return next(MemoryQueueDataPath.DEFAULT_TIME_OUT, MemoryQueueDataPath.DEFAULT_TIME_UNIT);

  }


  /**
   *
   *
   * @param timeout  - the timeout to wait (only for {@link MemoryDataPathType#TYPE_BLOCKED_QUEUE | queue structure)
   * @param timeUnit - the timeunit to wait (only for queue structure)
   * @return true if there is still an element or false
   */
  @Override
  public boolean next(Integer timeout, TimeUnit timeUnit) {


    assert timeout != null : "The timeout should not be null for a queue";
    assert timeUnit != null : "The time unit  should not be null for a queue";
    try {

      currentRow = tabular.poll(timeout, timeUnit);
      if (currentRow != null) {
        rowIndex++;
        return true;
      } else {
        return false;
      }

    } catch (InterruptedException e) {
      this.selectStreamListener.addException(e);
      throw new RuntimeException(e);
    }

  }

  @Override
  public void close() {

  }


  @Override
  public String getString(int columnIndex) {

    final int index = columnIndex;
    if (index < currentRow.size()) {
      final Object o = currentRow.get(index);
      if (o == null) {
        return null;
      } else {
        return o.toString();
      }
    } else {
      return "";
    }
  }


  @Override
  public long getRow() {
    return rowIndex + 1;
  }


  @Override
  public Object getObject(int columnIndex) {
    return currentRow.get(columnIndex);
  }

  @Override
  public RelationDef getSelectDataDef() {
    return memoryQueueDataPath.getDataDef();
  }


  @Override
  public Double getDouble(int columnIndex) {
    return (double) getObject(columnIndex);
  }

  @Override
  public Clob getClob(int columnIndex) {
    throw new RuntimeException("Not Yet Implemented");
  }

  @Override
  public Integer getInteger(int columnIndex) {
    return (Integer) getObject(columnIndex);
  }

  @Override
  public Object getObject(String columnName) {
    throw new RuntimeException("Not yet implemented");
  }

  @Override
  public List<Object> getObjects() {
    return currentRow;
  }

  @Override
  public void beforeFirst() {

    throw new RuntimeException("The Type (" + memoryQueueDataPath.getType() + ") of the data path (" + memoryQueueDataPath + ") does not support going back to the first argument");

  }

  @Override
  public void execute() {
    // nothing to do here
  }



}
