package net.bytle.db.memory.queue;

import net.bytle.db.memory.MemoryVariableManager;
import net.bytle.db.memory.MemoryVariableManagerProvider;
import net.bytle.db.memory.list.MemoryListManager;

public class MemoryQueueManagerProvider extends MemoryVariableManagerProvider {

  static private MemoryListManager memoryqueueManager;

  @Override
  public Boolean accept(String type) {
    return type.equals(MemoryQueueDataPath.TYPE);
  }

  @Override
  public MemoryVariableManager getMemoryVariableManager() {
    if (memoryqueueManager == null) {
      memoryqueueManager = new MemoryListManager();
    }
    return memoryqueueManager;
  }

}
