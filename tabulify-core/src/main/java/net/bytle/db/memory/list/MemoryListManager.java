package net.bytle.db.memory.list;

import net.bytle.db.memory.MemoryConnection;
import net.bytle.db.memory.MemoryVariableManager;

public class MemoryListManager implements MemoryVariableManager {


  @Override
  public MemoryListDataPath createDataPath(MemoryConnection memoryConnection, String path) {
    return new MemoryListDataPath(memoryConnection,path);
  }


}
