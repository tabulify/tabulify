package net.bytle.db.gen;

import net.bytle.db.memory.MemoryDataPath;
import net.bytle.db.memory.MemoryDataStore;
import net.bytle.db.memory.MemoryVariableManager;
import net.bytle.db.stream.InsertStream;
import net.bytle.db.stream.SelectStream;

public class GenMemManager implements MemoryVariableManager {

  @Override
  public SelectStream getSelectStream(MemoryDataPath memoryDataPath) {
    return new GenSelectStream(memoryDataPath);
  }

  @Override
  public InsertStream getInsertStream(MemoryDataPath memoryDataPath) {
    throw new RuntimeException("You can't insert in a generation data path");
  }

  @Override
  public MemoryDataPath createDataPath(MemoryDataStore memoryDataStore, String path) {
    return null;
  }

  @Override
  public void create(MemoryDataPath memoryDataPath) {
    // NAP
  }

  @Override
  public void truncate(MemoryDataPath memoryDataPath) {
    // NAP
  }

  @Override
  public long size(MemoryDataPath memoryDataPath) {
    throw new RuntimeException("To implement");
  }

}
