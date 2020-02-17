package net.bytle.db.gen.memory;

import net.bytle.db.gen.DataGenerator;
import net.bytle.db.gen.GenDataPath;
import net.bytle.db.gen.GenSelectStream;
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
    // We need to create it in the store to show that it exists
    memoryDataPath.getDataStore().getMemoryStore().put(memoryDataPath, null);
  }

  @Override
  public void truncate(MemoryDataPath memoryDataPath) {
    // NAP
  }

  @Override
  public long size(MemoryDataPath memoryDataPath) {

    DataGenerator dataGenerator =  DataGenerator.of((GenDataPath) memoryDataPath);
    return 0;

  }

}
