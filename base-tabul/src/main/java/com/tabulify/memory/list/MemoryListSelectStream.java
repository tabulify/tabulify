package com.tabulify.memory.list;

import com.tabulify.memory.MemoryDataPathAbs;
import com.tabulify.memory.MemoryDataPathType;
import com.tabulify.model.ColumnDef;
import com.tabulify.model.RelationDef;
import com.tabulify.stream.SelectStream;
import com.tabulify.stream.SelectStreamAbs;

import java.sql.Clob;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Select implementation
 */
public class MemoryListSelectStream extends SelectStreamAbs implements SelectStream {

  private final List<List<?>> values;
  @SuppressWarnings("FieldCanBeLocal")
  private final MemoryDataPathAbs memoryDataPath;

  // Index is used for a list for a queue
  private int rowIndex = -1;
  private List<?> currentRow;

  MemoryListSelectStream(MemoryListDataPath memoryListDataPath) {
    super(memoryListDataPath);
    this.memoryDataPath = memoryListDataPath;
    List<List<?>> values = memoryListDataPath.getValues();
    if (values == null) {
      throw new RuntimeException("The memory data path (" + memoryListDataPath + ") does not exist (or was not created before insertion)");
    }
    this.values = values;
  }


  @Override
  public boolean next() {

    return next(null, null);

  }


  /**
   * @param timeout  - the timeout to wait (only for {@link MemoryDataPathType#TYPE_BLOCKED_QUEUE | queue structure)
   * @param timeUnit - the timeunit to wait (only for queue structure)
   * @return true if there is still an element or false
   */
  @Override
  public boolean next(Integer timeout, TimeUnit timeUnit) {


    if (rowIndex >= values.size() - 1) {
      return false;
    }
    rowIndex++;
    currentRow = values.get(rowIndex);
    return true;

  }


  @Override
  public long getRecordId() {
    return rowIndex + 1;
  }


  @Override
  public Object getObject(ColumnDef columnDef) {
    if (currentRow == null) {
      throw new IllegalStateException("The current row is null. Did you use next to advance the cursor?");
    }
    int index = columnDef.getColumnPosition() - 1;
    return currentRow.get(index);
  }

  @Override
  public RelationDef getRuntimeRelationDef() {
    return this.getDataPath().getOrCreateRelationDef();
  }


  @Override
  public Clob getClob(int columnIndex) {
    throw new RuntimeException("Not Yet Implemented");
  }


  @Override
  public List<?> getObjects() {
    return currentRow;
  }

  @Override
  public void beforeFirst() {
    rowIndex = -1;
  }

  private boolean isClosed = false;

  @Override
  public void close() {
    this.isClosed = true;
  }

  @Override
  public boolean isClosed() {
    return this.isClosed;
  }

}
