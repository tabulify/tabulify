package net.bytle.db.memory;

import net.bytle.db.spi.DataPath;
import net.bytle.db.stream.InsertStream;
import net.bytle.db.stream.SelectStream;

/**
 * A memory data path does not have any special property
 *
 * The memory data path are managed by the java garbage collector
 * All store operations (such as truncate, size, ...) are then on the memory data path variable
 *
 */
public interface MemoryDataPath extends DataPath {


  String PATH_SEPARATOR = "/";

  MemoryDataStore getDataStore();
  MemoryDataPath getDataPath(String... parts);
  MemoryDataPath resolve(String... names);


  void truncate();

  long size();

  /**
   * Create (ie initialize the variable)
   */
  void create();

  InsertStream getInsertStream();

  SelectStream getSelectStream();



}
