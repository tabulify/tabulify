package net.bytle.db;

import net.bytle.db.database.DataStore;
import net.bytle.db.database.Database;
import net.bytle.db.database.Databases;
import net.bytle.db.database.FileDataStore;
import net.bytle.db.memory.MemorySystemProvider;
import net.bytle.db.spi.DataPath;
import net.bytle.db.uri.DataUri;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A tabular is a global domain.
 * It's the entry point of every tabular/data application
 * It has knowledge of:
 * * the {@link DatabasesStore}
 * * and of the {@link net.bytle.db.database.Database}
 * and therefore is the main entry to create a data path from an URI
 * * a datastore object
 * * a connection
 * * or
 */
public class Tabular implements AutoCloseable {


  /**
   * The local file database name as also stated
   * by the {@link Path#getFileSystem()}
   */
  public static final String LOCAL_FILE_DATABASE = "file";
  public static final String MEMORY_DATASTORE = "memory";

  /**
   * The memory database
   */
  public static final String DEFAULT_DATASTORE = MEMORY_DATASTORE;


  DatabasesStore databaseStore = null;

  Map<String, Database> dataStores = new HashMap<>();

  public String getDefaultDatastore() {
    return DEFAULT_DATASTORE;
  }

  public static Tabular tabular() {
    return new Tabular();
  }


  public DataPath ofPath(Path path) {
    return null;
  }

  public List<Database> getDataStores() {
    return new ArrayList<>(dataStores.values());
  }

  /**
   * @param dataUri - A data uri defining the first data path
   * @param parts   - The child of the first one if any
   * @return
   */
  public DataPath getDataPath(String dataUri, String... parts) {

    // First argument
    if (!dataUri.contains(DataUri.AT_STRING)) {
      dataUri = dataUri + DataUri.AT_STRING + DEFAULT_DATASTORE;
    }
    DataUri dataUriObj = DataUri.of(dataUri);
    String dataStoreName = dataUriObj.getDataStore();
    DataPath dataUriPath = getDataStore(dataStoreName)
      .getDataPath(dataUriObj.getPath());

    // Second argument to get the childs
    for (int i = 0; i < parts.length; i++) {
      dataUriPath = dataUriPath.getDataSystem().getChildOf(dataUriPath, parts[i]);
    }
    return dataUriPath;

  }

  private DataStore getDataStore(String dataStoreName) {

    switch (dataStoreName) {
      case MEMORY_DATASTORE:
        return Databases.of(MEMORY_DATASTORE)
          .setConnectionString(MemorySystemProvider.SCHEME);
      case LOCAL_FILE_DATABASE:
        return Databases.of(LOCAL_FILE_DATABASE).setConnectionString(Paths.get(".").toUri().toString());
      default:
        return dataStores.get(dataStoreName);
    }

  }

  public DatabasesStore getDataStoreVault() {
    return this.databaseStore;
  }

  public Tabular setDataStoreVault(Path storagePath) {
    databaseStore = DatabasesStore.of(storagePath);
    databaseStore.getDataStores().forEach(
      ds -> {
        dataStores.put(ds.getName(), ds);
      }
    );
    return this;
  }

  public Database getOrCreateDataStore(String dataStoreName) {
    Database dataStore = dataStores.get(dataStoreName);
    if (dataStore == null) {
      dataStore = Database.of(dataStoreName);
      dataStores.put(dataStore.getName(), dataStore);
    }
    return dataStore;
  }

  /**
   * @param dataUri - a pattern data uri (ie the path is a glob pattern)
   * @return a list of path that match the pattern
   * Example dim*@db will return all object with a name that start with `dim` for the data store `db`
   */
  public List<DataPath> select(String dataUri) {

    if (!dataUri.contains(DataUri.AT_STRING)) {
      dataUri = dataUri + DataUri.AT_STRING + DEFAULT_DATASTORE;
    }

    DataUri dataUriObj = DataUri.of(dataUri);
    String dataStoreName = dataUriObj.getDataStore();
    DataPath currentDataPath = getDataStore(dataStoreName)
      .getCurrentDataPath();

    String glob = dataUriObj.getPath();

    return currentDataPath.getDataSystem().getDescendants(currentDataPath, glob);

  }

  public void close()  {
    for (Database dataStore : dataStores.values()) {
      try {
        dataStore.getDataSystem().close();
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }

  public DataPath getDataPath(Path outputPath) {
    return
      new FileDataStore(outputPath)
        .getCurrentDataPath();
  }

  /**
   * Use the default storage location
   *
   * @return
   */
  public Tabular withDefaultStorage() {
    setDataStoreVault(DatabasesStore.DEFAULT_STORAGE_FILE);
    return this;
  }
}
