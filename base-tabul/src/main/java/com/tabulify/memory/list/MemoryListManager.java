package com.tabulify.memory.list;

import com.tabulify.memory.MemoryConnection;
import com.tabulify.memory.MemoryVariableManager;

public class MemoryListManager implements MemoryVariableManager {


  @Override
  public MemoryListDataPath createDataPath(MemoryConnection memoryConnection, String path) {
    return new MemoryListDataPath(memoryConnection,path);
  }


}
