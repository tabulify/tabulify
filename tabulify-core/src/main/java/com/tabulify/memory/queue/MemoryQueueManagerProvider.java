package com.tabulify.memory.queue;

import com.tabulify.memory.MemoryDataPathType;
import net.bytle.type.MediaType;
import com.tabulify.memory.MemoryVariableManager;
import com.tabulify.memory.MemoryVariableManagerProvider;

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
