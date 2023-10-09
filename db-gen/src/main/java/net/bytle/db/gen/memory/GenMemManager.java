package net.bytle.db.gen.memory;

import net.bytle.db.memory.MemoryDataPath;
import net.bytle.db.memory.MemoryConnection;
import net.bytle.db.memory.MemoryVariableManager;

public class GenMemManager implements MemoryVariableManager {


  @Override
  public MemoryDataPath createDataPath(MemoryConnection memoryConnection, String path) {

    return new GenMemDataPath(memoryConnection, path);

  }



}
