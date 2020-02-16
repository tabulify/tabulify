package net.bytle.db.gen;

import net.bytle.db.memory.MemoryDataPath;
import net.bytle.db.memory.MemoryDataPathAbs;
import net.bytle.db.memory.MemoryDataStore;

/**
 * A path specifically created for test
 * when the data gen path is created without a file
 *
 * If you use a file, use the {@link GenDataPath}
 */
public class GenMemDataPath extends MemoryDataPathAbs implements MemoryDataPath {


  private static MemoryDataStore memoryDataStore;

  public GenMemDataPath(MemoryDataStore memoryDataStore, String path) {
    super(memoryDataStore, path);
  }


  protected static GenMemDataPath of(String path) {
    if (memoryDataStore == null){
      memoryDataStore = new MemoryDataStore("gen", "gen");
    }
    return new GenMemDataPath(memoryDataStore, path);
  }

  @Override
  public GenDataDef getDataDef() {
    return new GenDataDef(this);
  }

  @Override
  public String getType() {
    return GenDataDef.TYPE;
  }
}
