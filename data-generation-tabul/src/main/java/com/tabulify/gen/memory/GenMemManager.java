package com.tabulify.gen.memory;

import com.tabulify.gen.GenDataPath;
import com.tabulify.memory.MemoryConnection;
import com.tabulify.memory.MemoryDataPath;
import com.tabulify.memory.MemoryVariableManager;

public class GenMemManager implements MemoryVariableManager {


  @Override
  public MemoryDataPath createDataPath(MemoryConnection memoryConnection, String path) {

    return new GenDataPath(memoryConnection, path);

  }



}
