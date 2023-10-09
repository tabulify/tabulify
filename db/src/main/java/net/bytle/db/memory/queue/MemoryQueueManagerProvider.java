package net.bytle.db.memory.queue;

import net.bytle.db.memory.MemoryDataPathType;
import net.bytle.type.MediaType;
import net.bytle.db.memory.MemoryVariableManager;
import net.bytle.db.memory.MemoryVariableManagerProvider;

public class MemoryQueueManagerProvider extends MemoryVariableManagerProvider {

  static private MemoryQueueManager memoryqueueManager;

  @Override
  public Boolean accept(MediaType type) {
    return type.toString().equals(MemoryDataPathType.QUEUE.toString());
  }

  @Override
  public MemoryVariableManager getMemoryVariableManager() {
    if (memoryqueueManager == null) {
      memoryqueueManager = new MemoryQueueManager();
    }
    return memoryqueueManager;
  }

}
