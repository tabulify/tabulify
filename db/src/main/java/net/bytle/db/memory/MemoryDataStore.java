package net.bytle.db.memory;

import net.bytle.db.database.DataStore;
import net.bytle.db.spi.DataPath;
import net.bytle.db.uri.DataUri;

public class MemoryDataStore extends DataStore {


  private final MemoryDataSystem memoryDataSystem;

  public MemoryDataStore(String name, String connectionString, MemoryDataSystem memoryDataSystem) {
    super(name, connectionString);
    this.memoryDataSystem = memoryDataSystem;
  }

  @Override
  public MemoryDataSystem getDataSystem() {
    return memoryDataSystem;
  }

  @Override
  public DataPath getDataPath(String... parts) {

    DataUri dataUri = DataUri.of(String.join(MemoryDataPath.PATH_SEPARATOR, parts) + DataUri.AT_STRING + this.getName());
    MemoryDataPath memoryDataPath = MemoryDataPath.of(this, dataUri.getPath());
    return memoryDataPath;

  }

  @Override
  public MemoryDataPath getCurrentDataPath() {
    return MemoryDataPath.of(this,"");
  }
}
