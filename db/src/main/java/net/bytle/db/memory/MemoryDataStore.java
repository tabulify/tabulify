package net.bytle.db.memory;

import net.bytle.db.database.DataStore;
import net.bytle.db.spi.TableSystem;

public class MemoryDataStore extends DataStore {


  private final MemoryDataSystem memoryDataSystem;

  public MemoryDataStore(String name, String connectionString, MemoryDataSystem memoryDataSystem) {
    super(name, connectionString);
    this.memoryDataSystem = memoryDataSystem;
  }

  @Override
  public TableSystem getDataSystem() {
    return memoryDataSystem;
  }


}
