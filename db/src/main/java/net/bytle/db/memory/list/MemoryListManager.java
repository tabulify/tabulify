package net.bytle.db.memory.list;

import net.bytle.db.memory.MemoryDataStore;
import net.bytle.db.memory.MemoryVariableManager;

public class MemoryListManager implements MemoryVariableManager {


  @Override
  public MemoryListDataPath createDataPath(MemoryDataStore memoryDataStore, String path) {
    return new MemoryListDataPath(memoryDataStore,path);
  }


}
