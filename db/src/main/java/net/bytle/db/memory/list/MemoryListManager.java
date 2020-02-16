package net.bytle.db.memory.list;

import net.bytle.db.memory.MemoryDataPath;
import net.bytle.db.memory.MemoryDataPathAbs;
import net.bytle.db.memory.MemoryDataStore;
import net.bytle.db.memory.MemoryVariableManager;
import net.bytle.db.stream.InsertStream;
import net.bytle.db.stream.SelectStream;

import java.util.ArrayList;

public class MemoryListManager implements MemoryVariableManager {

  @Override
  public SelectStream getSelectStream(MemoryDataPath memoryDataPath) {
    return new MemoryListSelectStream((MemoryListDataPath) memoryDataPath);
  }

  @Override
  public InsertStream getInsertStream(MemoryDataPath memoryDataPath) {
    return new MemoryListInsertStream((MemoryListDataPath) memoryDataPath);
  }

  @Override
  public MemoryDataPathAbs createDataPath(MemoryDataStore memoryDataStore, String path) {
    return new MemoryListDataPath(memoryDataStore,path);
  }

  @Override
  public void create(MemoryDataPath memoryDataPath) {
    memoryDataPath.getDataStore().getMemoryStore().put(memoryDataPath, new ArrayList<ArrayList<Object>>());
  }

  @Override
  public void truncate(MemoryDataPath memoryDataPath) {
    create(memoryDataPath);
  }

  @Override
  public long size(MemoryDataPath memoryDataPath) {
    ArrayList<ArrayList<Object>> values= (ArrayList<ArrayList<Object>>) memoryDataPath.getDataStore().getMemoryStore().getValue(memoryDataPath);
    return values.size();
  }

}
