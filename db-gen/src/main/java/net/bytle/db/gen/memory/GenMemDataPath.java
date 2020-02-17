package net.bytle.db.gen.memory;

import net.bytle.db.gen.GenDataDef;
import net.bytle.db.gen.GenDataPath;
import net.bytle.db.memory.MemoryDataPath;
import net.bytle.db.memory.MemoryDataPathAbs;
import net.bytle.db.memory.MemoryDataStore;

/**
 * A path specifically created for test
 * when the data gen path is created without a file
 *
 * If you use a file, use the {@link GenDataPath}
 */
public class GenMemDataPath extends MemoryDataPathAbs implements MemoryDataPath, GenDataPath {


  private static MemoryDataStore memoryDataStore;
  private GenDataDef genDataDef;

  public GenMemDataPath(MemoryDataStore memoryDataStore, String path) {
    super(memoryDataStore, path);
  }


  public static GenMemDataPath of(String path) {
    if (memoryDataStore == null){
      memoryDataStore = new MemoryDataStore("gen", "gen");
    }
    return new GenMemDataPath(memoryDataStore, path);
  }

  @Override
  public GenDataDef getDataDef() {
    if (genDataDef == null){
      genDataDef = new GenDataDef(this);
    }
    return genDataDef;
  }

  @Override
  public String getType() {
    return GenDataDef.TYPE;
  }


}
