package net.bytle.db.memory.queue;

import net.bytle.db.memory.MemoryDataPath;
import net.bytle.db.memory.MemoryDataPathAbs;
import net.bytle.db.memory.MemoryDataStore;
import net.bytle.db.memory.MemoryVariableManager;
import net.bytle.db.stream.InsertStream;
import net.bytle.db.stream.SelectStream;

import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;

public class MemoryQueueManager implements MemoryVariableManager {

  @Override
  public SelectStream getSelectStream(MemoryDataPath memoryDataPath) {
    return new MemoryQueueSelectStream((MemoryQueueDataPath) memoryDataPath);
  }

  @Override
  public InsertStream getInsertStream(MemoryDataPath memoryDataPath) {
    return new MemoryQueueInsertStream((MemoryQueueDataPath) memoryDataPath);
  }

  @Override
  public MemoryDataPathAbs createDataPath(MemoryDataStore memoryDataStore, String path) {
    return new MemoryQueueDataPath(memoryDataStore,path);
  }

  @Override
  public void create(MemoryDataPath memoryDataPath) {
    MemoryQueueDataPath memoryQueueDataPath = (MemoryQueueDataPath) memoryDataPath;
    memoryDataPath.getDataStore().getMemoryStore().put(memoryDataPath, new ArrayBlockingQueue<List<Object>>(memoryQueueDataPath.getCapacity()));
  }

  @Override
  public void truncate(MemoryDataPath memoryDataPath) {
    create(memoryDataPath);
  }

  @Override
  public long size(MemoryDataPath memoryDataPath) {
    ArrayBlockingQueue<List<Object>> values = (ArrayBlockingQueue<List<Object>>) memoryDataPath.getDataStore().getMemoryStore().getValue(memoryDataPath);
    return values.size();
  }

}
