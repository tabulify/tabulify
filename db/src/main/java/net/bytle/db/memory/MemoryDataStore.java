package net.bytle.db.memory;

import net.bytle.db.database.DataStore;
import net.bytle.db.database.JdbcDataType.DataTypesJdbc;
import net.bytle.db.model.DataType;
import net.bytle.db.spi.DataPath;
import net.bytle.db.spi.ProcessingEngine;
import net.bytle.db.uri.DataUri;

public class MemoryDataStore extends DataStore {

  private MemoryStore memoryStore;
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

  @Override
  public DataPath getQueryDataPath(String query) {
    throw new RuntimeException("Query is not yet supported on memory structure");
  }

  @Override
  public Integer getMaxWriterConnection() {
    return 1;
  }


  @Override
  public ProcessingEngine getProcessingEngine() {
    throw new RuntimeException("A processing engine is not yet supported on memory structure");
  }

  public MemoryStore getMemoryStore() {
    if (memoryStore == null){
      memoryStore = new MemoryStore();
    }
    return memoryStore;
  }
}
