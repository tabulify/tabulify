package net.bytle.db.memory;

import net.bytle.db.model.TableDef;
import net.bytle.db.spi.DataPath;
import net.bytle.db.spi.SelectStreamAbs;
import net.bytle.db.stream.SelectStream;

import java.sql.Clob;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Select implementation
 */
public class MemorySelectStream extends SelectStreamAbs implements SelectStream {

  private Collection<? extends List> tabular;
  private final MemoryDataPath memoryDataPath;

  // Index is used for a list for a queue
  private int rowIndex = -1;
  List<Object> currentRow;

  MemorySelectStream(MemoryDataPath memoryDataPath) {
    super(memoryDataPath);
    this.memoryDataPath = memoryDataPath;
    final MemoryStore memoryStore = memoryDataPath.getDataSystem().getMemoryStore();
    tabular = memoryStore.getValues(memoryDataPath);
    if (tabular == null) {
      switch (memoryDataPath.getType()) {
        case TYPE_BLOCKED_QUEUE:
          this.tabular = new ArrayBlockingQueue<>(memoryDataPath.getCapacity());
          break;
        case TYPE_LIST:
          this.tabular = new ArrayList<>();
          break;
        default:
          throw new RuntimeException("Type (" + memoryDataPath.getType() + ") is unknown for this memory data path (" + memoryDataPath + ")");
      }
      memoryStore.put(memoryDataPath, tabular);
    }
  }


  @Override
  public List<DataPath> getReference() {
    throw new RuntimeException("Not implemented");
  }

  @Override
  public boolean next() {

    return next(MemoryDataPath.DEFAULT_TIME_OUT, MemoryDataPath.DEFAULT_TIME_UNIT);

  }


  /**
   * For the following {@link MemoryDataPath#setType | data structure }
   * * a list, retrieve the element and returns false if this is the end of the list
   * * a queue, retrieves and removes the head and returns false if an element can not be retrieve in the specified timeout
   *
   * @param timeout  - the timeout to wait (only for {@link MemoryDataPathType#TYPE_BLOCKED_QUEUE | queue structure)
   * @param timeUnit - the timeunit to wait (only for queue structure)
   * @return true if there is still an element or false
   */
  @Override
  public boolean next(Integer timeout, TimeUnit timeUnit) {


    try {
      switch (memoryDataPath.getType()) {
        case TYPE_BLOCKED_QUEUE:
          assert timeout != null : "The timeout should not be null for a queue";
          assert timeUnit != null : "The time unit  should not be null for a queue";

          currentRow = ((ArrayBlockingQueue<? extends List>) tabular).poll(timeout, timeUnit);
          if (currentRow != null) {
            return true;
          } else {
            return false;
          }
        case TYPE_LIST:
          if (rowIndex >= tabular.size() - 1) {
            return false;
          } else {
            rowIndex++;
            currentRow = ((ArrayList<? extends List>) tabular).get(rowIndex);
            return true;
          }
        default:
          throw new RuntimeException("Type (" + memoryDataPath.getType() + ") is unknown for this memory data path (" + memoryDataPath + ")");
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
  public int getRow() {
    return rowIndex + 1;
  }


  @Override
  public Object getObject(int columnIndex) {
    return currentRow.get(columnIndex);
  }

  @Override
  public TableDef getSelectDataDef() {
    return memoryDataPath.getDataDef();
  }


  @Override
  public double getDouble(int columnIndex) {
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
    switch (memoryDataPath.getType()) {
      case TYPE_LIST:
        rowIndex = -1;
        break;
      default:
        throw new RuntimeException("The Type (" + memoryDataPath.getType() + ") of the data path (" + memoryDataPath + ") does not support going back to the first argument");
    }
  }



}
