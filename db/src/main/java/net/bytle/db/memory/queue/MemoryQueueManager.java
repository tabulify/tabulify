package net.bytle.db.memory.queue;

import net.bytle.db.memory.MemoryDataPathAbs;
import net.bytle.db.memory.MemoryDataStore;
import net.bytle.db.memory.MemoryVariableManager;

public class MemoryQueueManager implements MemoryVariableManager {

  @Override
  public MemoryDataPathAbs createDataPath(MemoryDataStore memoryDataStore, String path) {
    return new MemoryQueueDataPath(memoryDataStore,path);
  }



}
