package net.bytle.db;


import net.bytle.crypto.Protector;
import net.bytle.db.database.DataStore;
import net.bytle.fs.Fs;
import net.bytle.regexp.Globs;
import org.ini4j.Ini;
import org.ini4j.Wini;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * A database store implementation based on ini file
 * If a password is saved a passphrase should be provided
 */
public class DatastoreVault implements AutoCloseable {

  protected static final Logger LOGGER = LoggerFactory.getLogger(DatastoreVault.class);

  static Set<DataStore> internalDataStores = new HashSet<>();

  public static final String SQLITE = "sqlite";
  public static final String ORACLE = "oracle";
  public static final String SQLSERVER = "sqlserver";
  public static final String MYSQL = "mysql";
  public static final String POSTGRESQL = "postgresql";
  public static final String SQLITE_TARGET = "sqlite_target";

  static {

    internalDataStores.add(
      DataStore.of(SQLITE, getSqliteConnectionString(SQLITE))
        .addProperty("driver", "org.sqlite.JDBC")
        .setDescription("The sqlite default data store connection")
    );

    internalDataStores.add(
      DataStore.of(SQLITE_TARGET, getSqliteConnectionString(SQLITE_TARGET))
        .addProperty("driver", "org.sqlite.JDBC")
        .setDescription("The default sqlite target data store (Sqlite cannot read and write with the same connection)")
    );

    internalDataStores.add(
      DataStore.of(ORACLE, "jdbc:oracle:thin:@[host]:[port]/[servicename]")
        .setDescription("The default oracle data store")
        .addProperty("driver", "oracle.jdbc.OracleDriver")
    );

    internalDataStores.add(
      DataStore.of(SQLSERVER, "jdbc:sqlserver://localhost;databaseName=AdventureWorks;")
        .setDescription("The default sqlserver data store")
        .addProperty("driver", "com.microsoft.sqlserver.jdbc.SQLServerDriver")
        .setUser("sa")
        .setPassword("TheSecret1!")
    );

    internalDataStores.add(
      DataStore.of(MYSQL, "jdbc:mysql://[host]:[port]/[database]")
        .setDescription("The default mysql data store")
        .addProperty("driver", "com.mysql.jdbc.Driver")
    );

    // jdbc:postgresql://host:port/database?prop=value
    internalDataStores.add(
      DataStore.of(POSTGRESQL, "jdbc:postgresql://host:port/test?ssl=true")
        .setDescription("The default postgres data store")
        .addProperty("driver", "org.postgresql.Driver")
    );

  }


  /**
   * This is a passphrase used to encrypt the sample database password
   * Don't change this value
   */
  private static final String INTERNAL_PASSPHRASE = "r1zilGx22kRCUFjPGXbo";
  private String passphrase;
  private Path path;

  public static final Path DEFAULT_STORAGE_FILE = Paths.get(Fs.getAppData(Tabular.APP_NAME).toString(), "dsn.ini");

  // The in-memory dataStoreVaultRepresentation
  Map<String, DataStore> dataStores = new HashMap<>();

  /**
   * Constant
   */
  private static final String URL = "url";
  private static final String USER = "user";
  private static final String PASSWORD = "password";


  private DatastoreVault(Path path, String passphrase) {

    if (path != null) {
      this.path = path;
      this.passphrase = passphrase;
      DbLoggers.LOGGER_DB_ENGINE.info("Opening the database store (" + path.toAbsolutePath().toString() + ")");
      load();
    } else {
      throw new RuntimeException("The path of the data store vault file  should not be null");
    }
  }

  public static DatastoreVault of(Path path) {
    return new DatastoreVault(path,null);
  }

  public static DatastoreVault ofDefault() {
    return of(DEFAULT_STORAGE_FILE);
  }




  /**
   * Write the changes to the disk
   */
  public void flush() {

    try {
      if (!Files.exists(this.path)) {
        Fs.createFile(this.path);
      }
      Ini ini = new Ini(this.path.toFile());
      List<DataStore> dataStores = new ArrayList<>(this.dataStores.values());
      Collections.sort(dataStores);
      for (DataStore dataStore : dataStores) {
        ini.put(dataStore.getName(), URL, dataStore.getConnectionString());
        ini.put(dataStore.getName(), USER, dataStore.getUser());
        if (dataStore.getPassword() != null) {
          String definitivePassphrase = this.passphrase;
          if (definitivePassphrase == null) {
            if (internalDataStores.contains(dataStore)) {
              DataStore internalDataStore = internalDataStores.stream().filter(ds -> ds.getName().equals(dataStore.getName())).collect(Collectors.toList()).get(0);
              if (dataStore.getPassword().equals(internalDataStore.getPassword())) {
                definitivePassphrase = INTERNAL_PASSPHRASE;
              } else {
                throw new RuntimeException("A passphrase is mandatory when a password must be saved.");
              }
            } else {
              throw new RuntimeException("A passphrase is mandatory when a password must be saved.");
            }
          }
          String cipher = Protector.get(definitivePassphrase).encrypt(dataStore.getPassword());
          ini.put(dataStore.getName(), PASSWORD, cipher);
        }
        for (Map.Entry<String, String> property : dataStore.getProperties().entrySet()) {
          ini.put(dataStore.getName(), property.getKey(), property.getValue());
        }
      }
      ini.store();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }


  /**
   * Remove all databases metadata information that matchs on of the globPatterns
   *
   * @param globPatterns
   * @return a list of database name removed
   */
  public List<DataStore> removeDataStores(String... globPatterns) {

    List<DataStore> databasesToRemove = getDataStores(globPatterns);
    databasesToRemove.forEach(dataStore -> dataStores.remove(dataStore.getName()));

    return databasesToRemove;
  }

  /**
   * Removes all databases
   *
   * @return
   */
  public List<DataStore> removeAllDatabases() {

    List<DataStore> dataStoresRemoved = new ArrayList<>(this.dataStores.values());
    dataStores = new HashMap<>();
    return dataStoresRemoved;

  }

  /**
   * @return all databases
   */
  public List<DataStore> getDataStores() {
    return new ArrayList<>(dataStores.values());
  }

  /**
   * @param globPatterns
   * @return all databases that match this glob patterns
   */
  public List<DataStore> getDataStores(String... globPatterns) {
    return this.dataStores.values()
      .stream()
      .filter(ds -> Arrays.stream(globPatterns).anyMatch(gp -> {
        String pattern = Globs.toRegexPattern(gp);
        return ds.getName().matches(pattern);
      }))
      .collect(Collectors.toList());
  }

  public List<DataStore> getDataStores(List<String> globPatterns) {
    return getDataStores(globPatterns.toArray(new String[0]));
  }


  /**
   * @return a database by its name or NULL
   */
  public DatastoreVault load() {


    if (!Files.exists(this.path)) {

      dataStores = internalDataStores.stream()
        .collect(Collectors.toMap(DataStore::getName, DataStore::of));

    } else {


      Ini ini;
      try {
        ini = new Ini(this.path.toFile());
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
      for (String name : ini.keySet()) {
        Wini.Section iniSection = ini.get(name);
        String connectionString = iniSection.get(URL);
        DataStore dataStore = DataStore.of(name, connectionString);
        dataStores.put(name, dataStore);
        for (String propertyName : iniSection.keySet())
          switch (propertyName) {
            case (URL):
              break;
            case (USER):
              dataStore.setUser(iniSection.get(USER));
              break;
            case (PASSWORD):
              String password = null;
              String ciphertext = iniSection.get(PASSWORD);

              // Internal data store
              if (internalDataStores.contains(dataStore)) {
                try {
                  String internalPassphrase = INTERNAL_PASSPHRASE;
                  password = Protector.get(internalPassphrase).decrypt(ciphertext);
                } catch (Exception e) {
                  // If the password was changed, it can throw an error
                  LOGGER.warn("The password of the internal data store (" + dataStore.getName() + ") seems to have been changed.");
                }
              }

              if (this.passphrase == null && password == null) {
                throw new RuntimeException("To decrypt the password of the data store (" + dataStore + "), you need to give the passphrase.");
              }

              // Password still null
              if (password == null) {
                try {
                  password = Protector.get(this.passphrase).decrypt(ciphertext);
                } catch (Exception e) {
                  throw new RuntimeException("Unable to decrypt the password for the data store (" + dataStore + ") with the given passphrase");
                }
              }
              dataStore.setPassword(password);
              break;
            default:
              dataStore.addProperty(propertyName, iniSection.get(propertyName));
              break;
          }
      }
    }
    return this;
  }

  /**
   * @param dataStoreName
   * @return a JDBC connection string for the default data vault
   */
  static private String getSqliteConnectionString(String dataStoreName) {

    Path dbFile;
    // Trick to not have the user name in the output ie C:\Users\Username\...
    // The env value have a fake account
    final String bytle_db_databases_store = System.getenv("BYTLE_DB_SQLITE_PATH");
    if (bytle_db_databases_store != null) {
      dbFile = Paths.get(bytle_db_databases_store);
    } else {
      dbFile = Paths.get(Fs.getAppData(Tabular.APP_NAME).toAbsolutePath().toString(), dataStoreName + ".db");
    }
    Fs.createDirectoryIfNotExists(dbFile.getParent());
    String rootWindows = "///";
    return "jdbc:sqlite:" + rootWindows + dbFile.toString().replace("\\", "/");

  }

  /**
   * @param name
   * @return the removed datastore
   */
  public DataStore removeDataStore(String name) {
    DataStore dataStore = this.dataStores.remove(name);
    if (dataStore == null) {
      throw new RuntimeException("The database (" + name + ") is non existent and therefore cannot be removed.");
    }
    return dataStore;
  }

  public Path getPath() {
    return this.path;
  }

  public void removeDataStoreIfExists(String name) {
    if (exists(name)) {
      removeDataStore(name);
    }
  }

  /**
   * @param name
   * @return boolean
   */
  boolean exists(String name) {

    if (dataStores.get(name) != null) {
      return true;
    } else {
      return false;
    }
  }

  public void add(DataStore dataStore) {
    assert dataStores.get(dataStore.getName()) == null : "The data store (" + dataStore.getName() + ") exists already and cannot be added";
    if (dataStore.getConnectionString() == null) {
      throw new RuntimeException("A connection string (url) is mandatory to add a datastore, the data store (" + dataStore.getName() + ") does not have any.");
    }
    dataStores.put(dataStore.getName(), dataStore);
  }


  @Override
  public void close() {
    flush();
  }

  public DataStore getDataStore(String name) {
    return this.dataStores.get(name);
  }

  static public DatastoreVault of(Path storagePath, String passphrase) {
    return new DatastoreVault(storagePath,passphrase);
  }
}
