package net.bytle.db.memory;

import net.bytle.db.stream.InsertStream;
import net.bytle.db.stream.SelectStream;

/**
 * A variable manager is responsible for:
 *   * the instantiation of a file with a structure
 *   * the retrieving of a select and insert stream
 *
 *
 *
 * A variable manager for a defined type can be defined via the {@link MemoryVariableManagerProvider}
 *
 */
public interface MemoryVariableManager {


  SelectStream getSelectStream(MemoryDataPath memoryDataPath);

  InsertStream getInsertStream(MemoryDataPath memoryDataPath);

  MemoryDataPath createDataPath(MemoryDataStore memoryDataStore, String path);

  /**
   * Create the variable (instantiate)
   * For instance, if it's a list, create the ArrayList
   * @param memoryDataPath
   */
  void create(MemoryDataPath memoryDataPath);

  void truncate(MemoryDataPath memoryDataPath);

  long size(MemoryDataPath memoryDataPath);
}
