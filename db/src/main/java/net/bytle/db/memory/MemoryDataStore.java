package net.bytle.db.memory;

import net.bytle.db.DbLoggers;
import net.bytle.db.database.DataStore;
import net.bytle.db.memory.list.MemoryListDataPath;
import net.bytle.db.spi.DataPath;
import net.bytle.db.spi.ProcessingEngine;
import net.bytle.db.spi.Tabulars;
import net.bytle.type.Strings;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MemoryDataStore extends DataStore {

  static final String WORKING_PATH = "";
  private final MemoryDataSystem memoryDataSystem;

  /**
   * The data path of the data store are keep here
   * This is to be able to support the copy/merge of data defs
   * into another data path that have foreign key relationships
   * See {@link net.bytle.db.model.DataDefAbs#copyDataDef(DataPath)}
   */
  Map<String, MemoryDataPath> dataPaths = new HashMap<>();

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
  public MemoryDataPath getDefaultDataPath(String... parts) {

    return getTypedDataPath(MemoryListDataPath.TYPE, parts);

  }

  @Override
  public MemoryDataPath getTypedDataPath(String type, String... parts) {
    if (parts.length==0){
      throw new RuntimeException(
        Strings.multiline("You can't create a data path without name",
          "If you don't want to specify a name, use the getRandomDataPath function"));
    }
    String path = String.join("/",parts);
    MemoryDataPath memoryDataPath = this.dataPaths.get(path);
    if (memoryDataPath==null) {
      getManager(type).createDataPath(this, path);
      memoryDataPath = new MemoryListDataPath(this, path);
      this.dataPaths.put(path, memoryDataPath);
    }
    return memoryDataPath;
  }

  @Override
  public MemoryDataPath getCurrentDataPath() {
    return getDefaultDataPath(WORKING_PATH);
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



  public DataPath getAndCreateRandomDataPath() {
    DataPath dataPath = getDefaultDataPath(UUID.randomUUID().toString());
    Tabulars.create(dataPath);
    return dataPath;
  }

  @Override
  public void close() {
    super.close();
    // Delete all data paths
    this.dataPaths = null;
  }

  public MemoryVariableManager getManager(String type) {
    MemoryVariableManager memoryVariableManager = null;
    List<MemoryVariableManagerProvider> installedProviders = MemoryVariableManagerProvider.installedProviders();
    for (MemoryVariableManagerProvider structProvider : installedProviders) {
      if (structProvider.accept(type)) {
        memoryVariableManager = structProvider.getMemoryVariableManager();
        if (memoryVariableManager == null) {
          String message = "The returned variable manager is null for the provider (" + structProvider.getClass().toString() + ")";
          DbLoggers.LOGGER_DB_ENGINE.severe(message);
          throw new RuntimeException(message);
        }
      }
    }
    if (memoryVariableManager == null) {
      throw new RuntimeException("The type ("+type+") has no installed provider.");
    }
    return memoryVariableManager;
  }
}
