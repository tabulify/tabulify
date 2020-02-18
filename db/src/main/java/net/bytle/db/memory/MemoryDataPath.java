package net.bytle.db.memory;

import net.bytle.db.spi.DataPath;

/**
 * A memory data path does not have any special property
 */
public interface MemoryDataPath extends DataPath {


  String PATH_SEPARATOR = "/";

  MemoryDataStore getDataStore();
  MemoryDataPath getDataPath(String... parts);
  MemoryDataPath resolve(String... names);

}
