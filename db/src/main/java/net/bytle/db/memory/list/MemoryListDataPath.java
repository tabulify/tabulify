package net.bytle.db.memory.list;


import net.bytle.db.memory.MemoryDataPathAbs;
import net.bytle.db.memory.MemoryDataStore;

public class MemoryListDataPath extends MemoryDataPathAbs {


  /**
   * Type
   */
  public static final String TYPE = "LIST";

  public MemoryListDataPath(MemoryDataStore memoryDataStore, String path) {
    super(memoryDataStore, path);
  }

  public static MemoryListDataPath of(MemoryDataStore memoryDataStore, String path) {
    return new MemoryListDataPath(memoryDataStore, path);
  }

  public String getType() {
    return TYPE;
  }


}
