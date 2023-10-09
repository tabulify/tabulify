package net.bytle.db.memory.queue;

import net.bytle.db.memory.MemoryDataPathAbs;
import net.bytle.db.memory.MemoryConnection;
import net.bytle.db.memory.MemoryVariableManager;

public class MemoryQueueManager implements MemoryVariableManager {

  @Override
  public MemoryDataPathAbs createDataPath(MemoryConnection memoryConnection, String path) {
    return new MemoryQueueDataPath(memoryConnection,path);
  }



}
