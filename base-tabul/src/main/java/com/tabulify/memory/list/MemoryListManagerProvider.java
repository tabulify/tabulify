package com.tabulify.memory.list;

import com.tabulify.memory.MemoryDataPathType;
import com.tabulify.type.MediaType;
import com.tabulify.memory.MemoryVariableManager;
import com.tabulify.memory.MemoryVariableManagerProvider;

public class MemoryListManagerProvider extends MemoryVariableManagerProvider {

  static private MemoryListManager memoryListManager;

  @Override
  public Boolean accept(MediaType type) {
    return type.toString().equalsIgnoreCase(MemoryDataPathType.LIST.toString());
  }

  @Override
  public MemoryVariableManager getMemoryVariableManager() {
    if (memoryListManager == null) {
      memoryListManager = new MemoryListManager();
    }
    return memoryListManager;
  }

}
