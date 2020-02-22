package net.bytle.db.memory.list;

import net.bytle.db.memory.MemoryDataPathAbs;
import net.bytle.db.memory.MemoryDataPathType;
import net.bytle.db.model.RelationDef;
import net.bytle.db.stream.SelectStream;
import net.bytle.db.stream.SelectStreamAbs;

import java.sql.Clob;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Select implementation
 */
public class MemoryListSelectStream extends SelectStreamAbs implements SelectStream {

  private List<List<Object>> tabular;
  private final MemoryDataPathAbs memoryDataPath;

  // Index is used for a list for a queue
  private int rowIndex = -1;
  List<Object> currentRow;

  MemoryListSelectStream(MemoryListDataPath memoryListDataPath) {
    super(memoryListDataPath);
    this.memoryDataPath = memoryListDataPath;
    Object value = memoryListDataPath.getDataStore().getMemoryStore().getValue(memoryListDataPath);
    if (value == null){
      throw  new RuntimeException("The memory data path ("+memoryListDataPath+") does not exist (or was not created before insertion)");
    }
    this.tabular = (List<List<Object>>) value;
  }


  @Override
  public boolean next() {

    return next(null, null);

  }


  /**
   *
   * @param timeout  - the timeout to wait (only for {@link MemoryDataPathType#TYPE_BLOCKED_QUEUE | queue structure)
   * @param timeUnit - the timeunit to wait (only for queue structure)
   * @return true if there is still an element or false
   */
  @Override
  public boolean next(Integer timeout, TimeUnit timeUnit) {


    if (rowIndex >= tabular.size() - 1) {
      return false;
    } else {
      rowIndex++;
      currentRow = tabular.get(rowIndex);
      return true;
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
    return memoryDataPath.getDataDef();
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
        rowIndex = -1;
  }

  @Override
  public void execute() {
    // nothing to do here
  }


}
