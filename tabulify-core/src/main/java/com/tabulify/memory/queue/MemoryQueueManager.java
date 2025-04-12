package com.tabulify.memory.queue;

import com.tabulify.memory.MemoryDataPathAbs;
import com.tabulify.memory.MemoryConnection;
import com.tabulify.memory.MemoryVariableManager;

public class MemoryQueueManager implements MemoryVariableManager {

  @Override
  public MemoryDataPathAbs createDataPath(MemoryConnection memoryConnection, String path) {
    return new MemoryQueueDataPath(memoryConnection,path);
  }



}
