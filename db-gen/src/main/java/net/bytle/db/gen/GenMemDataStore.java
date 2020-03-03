package net.bytle.db.gen;

import net.bytle.db.gen.memory.GenMemDataPath;
import net.bytle.db.memory.MemoryDataStore;

public class GenMemDataStore extends MemoryDataStore {

  private static GenMemDataStore memoryDataStore;


  public GenMemDataStore(String name, String connectionString) {
    super(name, connectionString);
  }

  public static GenMemDataStore singleton() {
    if (memoryDataStore == null){
      memoryDataStore = new GenMemDataStore("gen", "gen");
    }
    return memoryDataStore;
  }

  @Override
  public GenMemDataPath getDefaultDataPath(String... parts) {
    return (GenMemDataPath) super.getTypedDataPath(GenDataDef.TYPE, parts);
  }

  @Override
  public GenMemDataPath getAndCreateRandomDataPath() {
    return (GenMemDataPath) super.getAndCreateRandomDataPath();
  }
}
