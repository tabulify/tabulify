package net.bytle.db;

import net.bytle.db.database.Database;
import net.bytle.db.spi.DataPath;
import net.bytle.db.spi.DataPaths;
import net.bytle.db.uri.DataUri;

import java.nio.file.Path;
import java.util.List;

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
public class Tabular {

  public static final String DEFAULT_DATASTORE = DatabasesStore.MEMORY_DATABASE;

  private static Tabular tabular;

  DatabasesStore databaseStore = null;

  public String getDefaultDatastore() {
    return DEFAULT_DATASTORE;
  }

  public static Tabular tabular() {
    if (tabular == null) {
      tabular = new Tabular();
    }
    return tabular;
  }


  public DataPath ofPath(Path path) {
    return null;
  }

  public List<Database> getDataStores() {
    if (databaseStore == null) {
      databaseStore = DatabasesStore.ofDefault();
    }
    return databaseStore.getDataStores();
  }

  /**
   *
   * @param dataUri - A data uri defining the first data path
   * @param parts - The child of the first one if any
   * @return
   */
  public DataPath getDataPath(String dataUri, String... parts) {

    if (!dataUri.contains(DataUri.AT_STRING)) {
      dataUri = dataUri + DataUri.AT_STRING + DEFAULT_DATASTORE;
    }
    DataUri dataUriObj = DataUri.of(dataUri);
    String dataStoreName = dataUriObj.getDataStore();
    Database database = getDataStore(dataStoreName);
    DataPath dataPath = DataPaths.of(database, dataUriObj);
    for (int i = 0; i < parts.length; i++) {
      dataPath = dataPath.getDataSystem().getChildOf(dataPath, parts[i]);
    }
    return dataPath;

  }

  private Database getDataStore(String dataStoreName) {
    if (databaseStore == null) {
      databaseStore = DatabasesStore.ofDefault();
    }
    return databaseStore.getDatabase(dataStoreName);
  }

}
