package net.bytle.db.memory;

import net.bytle.db.database.DataStore;
import net.bytle.db.memory.list.MemoryListDataPath;
import net.bytle.db.spi.DataPath;
import net.bytle.db.spi.ProcessingEngine;

public class MemoryDataStore extends DataStore {

  static final String WORKING_PATH = "";
  private MemoryStore memoryStore;
  private final MemoryDataSystem memoryDataSystem;

  public MemoryDataStore(String name, String connectionString) {
    super(name, connectionString);
    this.memoryDataSystem = MemoryDataSystem.of();
  }

  public static MemoryDataStore of(String name, String connectionString){
    return new MemoryDataStore(name,connectionString);
  }


  @Override
  public MemoryDataSystem getDataSystem() {
    return memoryDataSystem;
  }

  @Override
  public MemoryDataPath getDataPath(String... parts) {

    return this.getCurrentDataPath().resolve(parts);

  }

  @Override
  public MemoryDataPath getCurrentDataPath() {
    return new MemoryListDataPath(this,WORKING_PATH);
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
