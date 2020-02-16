package net.bytle.db;

import net.bytle.db.database.DataStore;
import net.bytle.db.engine.Queries;
import net.bytle.db.fs.FsDataStore;
import net.bytle.db.memory.MemorySystemProvider;
import net.bytle.db.model.RelationDef;
import net.bytle.db.model.TableDef;
import net.bytle.db.spi.DataPath;
import net.bytle.db.uri.DataUri;
import net.bytle.fs.Fs;
import net.bytle.regexp.Globs;
import net.bytle.type.Strings;

import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * A tabular is a global domain.
 * It's the entry point of every tabular/data application
 * It has knowledge of the {@link DatastoreVault}
 * and therefore is the main entry to create a data path from an URI
 * * a datastore object
 * * a connection
 * * or
 */
public class Tabular implements AutoCloseable {


  // Named used to create default path
  public static final String APP_NAME = "BytleDb";

  /**
   * The local file database name as also stated
   * by the {@link Path#getFileSystem()}
   */
  public static final String MEMORY_DATASTORE = "memory";
  public static final String TPCDS_DATASTORE = "tpcds";

  public static final String DEFAUT_URL = MemorySystemProvider.SCHEME;


  /**
   * The memory database
   */
  public static final String DEFAULT_DATASTORE = MEMORY_DATASTORE;


  DatastoreVault dataStoreVault = null;

  Map<String, DataStore> dataStores = new HashMap<>();
  private String passphrase;


  public Tabular() {

    // Intern datastore

    // Local Fs
    dataStores.put(FsDataStore.getLocalFileSystem().getName(), FsDataStore.getLocalFileSystem());

    // Memory
    DataStore memoryDataBase = DataStore.of(MEMORY_DATASTORE,MemorySystemProvider.SCHEME);
    dataStores.put(memoryDataBase.getName(), memoryDataBase);

    // TpcsDs
    DataStore tpcDs = DataStore.of(TPCDS_DATASTORE,TPCDS_DATASTORE)
      .addProperty("scale", "0.01");
    dataStores.put(tpcDs.getName(), tpcDs);

  }

  public static Tabular tabular() {
    return new Tabular();
  }


  public List<DataStore> getDataStores() {
    return new ArrayList<>(dataStores.values());
  }

  public List<DataStore> getDataStores(String... globPatterns) {
    return this.dataStores.values()
      .stream()
      .filter(ds -> Arrays.stream(globPatterns).anyMatch(gp -> {
        String pattern = Globs.toRegexPattern(gp);
        return ds.getName().matches(pattern);
      }))
      .collect(Collectors.toList());
  }

  /**
   * @param dataUri - A data uri defining the first data path
   * @param parts   - The child of the first one if any
   * @return
   */
  public DataPath getDataPath(String dataUri, String... parts) {
    assert dataUri!=null: "The first name of the data path should not be null";

    // First argument
    if (!dataUri.contains(DataUri.AT_STRING)) {
      dataUri = dataUri + DataUri.AT_STRING + DEFAULT_DATASTORE;
    }
    DataUri dataUriObj = DataUri.of(dataUri);
    String dataStoreName = dataUriObj.getDataStore();
    String path = dataUriObj.getPath();
    DataStore dataStore = getDataStore(dataStoreName);
    DataPath dataUriPath;
    if (path != null) {
      dataUriPath = dataStore.getDataPath(path);
    } else {
      dataUriPath = dataStore.getCurrentDataPath();
    }

    // Second argument to get the childs
    for (int i = 0; i < parts.length; i++) {
      dataUriPath = dataUriPath.getChild(parts[i]);
    }
    return dataUriPath;

  }

  /**
   * @param dataStoreName
   * @return a datastore
   * <p>
   * There is two specials data store always available (namely file and memory that store data respectively on the local file system and in memory)
   */
  public DataStore getDataStore(String dataStoreName) {

    return dataStores.get(dataStoreName);

  }

  public DatastoreVault getDataStoreVault() {
    return this.dataStoreVault;
  }

  public Tabular setDataStoreVault(Path storagePath) {

    if (passphrase != null) {
      dataStoreVault = dataStoreVault.of(storagePath,passphrase);
    } else {
      dataStoreVault = dataStoreVault.of(storagePath);
    }
    dataStoreVault.getDataStores().forEach(
      ds -> {
        dataStores.put(ds.getName(), ds);
      }
    );
    return this;
  }

  public DataStore getOrCreateDataStore(String dataStoreName) {
    DataStore dataStore = dataStores.get(dataStoreName);
    if (dataStore == null) {
      dataStore = DataStore.of(dataStoreName,DEFAUT_URL);
      dataStores.put(dataStore.getName(), dataStore);
    }
    return dataStore;
  }


  public void close() {
    for (DataStore dataStore : dataStores.values()) {
      try {
        // A data store that was not used will have no data system
        if (dataStore.isOpen()) {
          dataStore.close();
        }
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
    // Not really needed has the tabular object does not add any data store to the data store value
    // but this is a resource
    if (dataStoreVault!=null) {
      dataStoreVault.close();
    }
  }

  public DataPath getDataPath(Path path) {

    return FsDataStore.getLocalFileSystem().getDataPath(path);

  }


  /**
   * Use the default storage location
   *
   * @return
   */
  public Tabular withDefaultDataStoreVault() {
    setDataStoreVault(DatastoreVault.DEFAULT_STORAGE_FILE);
    return this;
  }

  public Tabular setPassphrase(String passphrase) {
    this.passphrase = passphrase;
    return this;
  }


  /**
   * An utility function that returns data paths selected from a data uri pattern (ie glob_pattern@datastore)
   * <p>
   * This function takes into account:
   * * the query uri pattern (ie queryPattern*.sql@datastore, `select 1@datastore`)
   * * and a entity uri pattern (ie table*@datastore)
   *
   * @param dataUriPattern
   * @return the data paths that the data uri pattern selects
   */
  public List<DataPath> select(String dataUriPattern) {
    List<DataPath> dataPathsToReturn = new ArrayList<>();
    if (!dataUriPattern.contains(DataUri.AT_STRING)) {
      dataUriPattern = dataUriPattern + DataUri.AT_STRING + DEFAULT_DATASTORE;
    }
    DataUri dataUri = DataUri.of(dataUriPattern);
    String pathInUri = dataUri.getPath();

    // Query URI with an inline query ?
    if (Queries.isQuery(pathInUri)) {
      DataPath inlineQueryDataPath = this
        .getDataStore(dataUri.getDataStore())
        .getQueryDataPath(pathInUri)
        .setDescription("Inline Query");
      dataPathsToReturn.add(inlineQueryDataPath);
    } else {
      // Query URI with a path that defines Sql File ?
      if (pathInUri.endsWith(".sql") || pathInUri.endsWith(TableDef.DATA_DEF_SUFFIX)) {

        List<Path> files = Fs.getFilesByGlob(pathInUri);
        if (files.size() == 0) {
          return dataPathsToReturn;
        }


        for (Path path : files) {
          if (pathInUri.endsWith(".sql")) {
            String query = Fs.getFileContent(path);
            if (Queries.isQuery(query)) {
              String queryName = Fs.getFileNameWithoutExtension(path);
              DataPath queryDataPath = this
                .getDataStore(dataUri.getDataStore())
                .getQueryDataPath(query)
                .setDescription(queryName);
              dataPathsToReturn.add(queryDataPath);
            } else {
              String msg = Strings.multiline("The query uri pattern (" + dataUriPattern + ") has selected the file (" + path + ") that seems to not contain a query",
                "The file content is: ",
                Strings.toStringNullSafe(query)
              );
              throw new RuntimeException(msg);
            }

          } else if (pathInUri.endsWith(TableDef.DATA_DEF_SUFFIX)) {
            DataPath dataDefDataPath = this
              .getDataStore(dataUri.getDataStore())
              .createOrMergeDataPathOfDataDef(path);
            dataPathsToReturn.add(dataDefDataPath);
          }
        }
      } else {
        // Normal data uri pattern
        String dataStoreName = dataUri.getDataStore();
        DataStore dataStore = getDataStore(dataStoreName);
        if (dataStore==null){
          throw new RuntimeException("No data store was found with the name ("+dataStoreName+")");
        }
        dataPathsToReturn = dataStore.select(pathInUri);

      }
    }
    return dataPathsToReturn;
  }

  /**
   * Return a data path from a data def file with the default data store
   * The name of the data path is the file name without the {@link TableDef#DATA_DEF_SUFFIX data def extension}
   *
   * @param dataDefPath
   * @return the data path with its meta
   */
  public DataPath getDataPathOfDataDef(Path dataDefPath) {
    return getDefaultDataStore().createOrMergeDataPathOfDataDef(dataDefPath);
  }

  public DataPath getDataPathOfDataDef(String name, RelationDef datadef) {
    return getDefaultDataStore().getCurrentDataPath().getChild(name, datadef);
  }

  public DataStore getDefaultDataStore() {
    return this.getDataStore(DEFAULT_DATASTORE);
  }

  public FsDataStore getLocalFileDataStore() {
    return FsDataStore.getLocalFileSystem();
  }
}
