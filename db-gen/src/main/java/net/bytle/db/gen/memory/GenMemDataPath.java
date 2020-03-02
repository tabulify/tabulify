package net.bytle.db.gen.memory;

import net.bytle.db.gen.GenDataDef;
import net.bytle.db.gen.GenDataPath;
import net.bytle.db.gen.GenSelectStream;
import net.bytle.db.memory.MemoryDataPath;
import net.bytle.db.memory.MemoryDataPathAbs;
import net.bytle.db.memory.MemoryDataStore;
import net.bytle.db.stream.InsertStream;
import net.bytle.db.stream.SelectStream;

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


  public static GenMemDataPath of(MemoryDataStore memoryDataStore, String path) {
    return new GenMemDataPath(memoryDataStore, path);
  }

  public static GenMemDataPath of(String path) {
    if (memoryDataStore == null){
      memoryDataStore = new MemoryDataStore("gen", "gen");
    }
    return new GenMemDataPath(memoryDataStore, path);
  }

  @Override
  public GenDataDef getOrCreateDataDef() {
    if (genDataDef == null){
      genDataDef = new GenDataDef(this);
      super.relationDef = genDataDef;
    }
    return genDataDef;
  }

  @Override
  public String getType() {
    return GenDataDef.TYPE;
  }


  @Override
  public void truncate() {
    // Nothing to do
  }

  @Override
  public long size() {
    return this.getOrCreateDataDef().getSize();
  }

  @Override
  public void create() {
    // Nothing to do
  }


  @Override
  public InsertStream getInsertStream() {
    throw new RuntimeException("You can't insert in a generation data path");
  }

  @Override
  public SelectStream getSelectStream() {
    return new GenSelectStream(this);
  }




}
