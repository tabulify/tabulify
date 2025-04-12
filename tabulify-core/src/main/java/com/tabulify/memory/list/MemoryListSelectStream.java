package com.tabulify.memory.list;

import com.tabulify.memory.MemoryDataPathAbs;
import com.tabulify.memory.MemoryDataPathType;
import com.tabulify.model.ColumnDef;
import com.tabulify.model.RelationDef;
import com.tabulify.stream.SelectStream;
import com.tabulify.stream.SelectStreamAbs;
import net.bytle.exception.NoColumnException;

import java.sql.Clob;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Select implementation
 */
public class MemoryListSelectStream extends SelectStreamAbs implements SelectStream {

  private final List<List<Object>> values;
  @SuppressWarnings("FieldCanBeLocal")
  private final MemoryDataPathAbs memoryDataPath;

  // Index is used for a list for a queue
  private int rowIndex = -1;
  private List<Object> currentRow;

  MemoryListSelectStream(MemoryListDataPath memoryListDataPath) {
    super(memoryListDataPath);
    this.memoryDataPath = memoryListDataPath;
    List<List<Object>> values = memoryListDataPath.getValues();
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
    } else {
      rowIndex++;
      currentRow = values.get(rowIndex);
      return true;
    }


  }

  @Override
  public void close() {

  }


  @Override
  public String getString(int columnIndex) {

    return getObject(columnIndex, String.class);

  }


  @Override
  public long getRow() {
    return rowIndex + 1;
  }


  @Override
  public Object getObject(int columnIndex) {
    return currentRow.get(columnIndex - 1);
  }

  @Override
  public RelationDef getRuntimeRelationDef() {
    return this.getDataPath().getOrCreateRelationDef();
  }


  @Override
  public Double getDouble(int columnIndex) {

    return getObject(columnIndex, Double.class);
  }

  @Override
  public Clob getClob(int columnIndex) {
    throw new RuntimeException("Not Yet Implemented");
  }

  @Override
  public Integer getInteger(int columnIndex) {

    return getObject(columnIndex, Integer.class);
  }

  @Override
  public Object getObject(String columnName) {
    ColumnDef columnDef;
    try {
      columnDef = this.getRuntimeRelationDef().getColumnDef(columnName);
    } catch (NoColumnException e) {
      return null;
    }
    Integer position = columnDef.getColumnPosition();
    return getObject(position, Object.class);
  }

  @Override
  public List<?> getObjects() {
    return currentRow;
  }

  @Override
  public void beforeFirst() {
    rowIndex = -1;
  }


}
