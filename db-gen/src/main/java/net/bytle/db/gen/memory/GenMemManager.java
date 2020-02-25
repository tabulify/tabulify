package net.bytle.db.gen.memory;

import net.bytle.db.memory.MemoryDataPath;
import net.bytle.db.memory.MemoryDataStore;
import net.bytle.db.memory.MemoryVariableManager;

public class GenMemManager implements MemoryVariableManager {


  @Override
  public MemoryDataPath createDataPath(MemoryDataStore memoryDataStore, String path) {

    return GenMemDataPath.of(memoryDataStore, path);

  }



}
