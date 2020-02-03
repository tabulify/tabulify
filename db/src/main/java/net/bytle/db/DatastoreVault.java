package net.bytle.db;


import net.bytle.crypto.Protector;
import net.bytle.db.database.DataStore;
import net.bytle.db.database.Database;
import net.bytle.fs.Fs;
import net.bytle.regexp.Globs;
import org.ini4j.Ini;
import org.ini4j.Profile;
import org.ini4j.Wini;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A database store implementation based on ini file
 * If a password is saved a passphrase should be provided
 */
public class DatastoreVault {

  protected static final Logger logger = LoggerFactory.getLogger(DatastoreVault.class);



  /**
   * This is a passphrase used to encrypt the sample database password
   * Don't change this value
   */
  private static final String INTERNAL_PASSPHRASE = "r1zilGx22kRCUFjPGXbo";
  private static final String INTERNAL_PASSPHRASE_KEY = "bdb_internal_passphrase";
  private String passphrase;
  private Path path;

  public static final Path DEFAULT_STORAGE_FILE = Paths.get(Fs.getAppData(Tabular.APP_NAME).toString(), "dsn.ini");

  /**
   * Constant
   */
  private static final String URL = "url";
  private static final String USER = "user";
  private static final String PASSWORD = "password";


  /**
   * The ini file were database information are saved to disk
   */
  private Ini ini;

  private DatastoreVault(Path path) {

    if (path != null) {
      this.path = path;
      DbLoggers.LOGGER_DB_ENGINE.info("Opening the database store (" + path.toAbsolutePath().toString() + ")");
    } else {
      throw new RuntimeException("The path store should not be null");
    }
  }

  public static DatastoreVault of(Path path) {
    return new DatastoreVault(path);
  }

  public static DatastoreVault ofDefault() {
    return of(DEFAULT_STORAGE_FILE);
  }


  public DatastoreVault setPassphrase(String passphrase) {
    this.passphrase = passphrase;
    return this;
  }

  /**
   * @param dataStore
   * @param internalPassphrase - to indicate that this is an built-in database and that the internal passphrase should be used - only called from this class
   * @return
   */
  private DatastoreVault save(DataStore dataStore, Boolean internalPassphrase) {

    Ini ini = getIniFile();
    ini.put(dataStore.getName(), URL, dataStore.getConnectionString());
    ini.put(dataStore.getName(), USER, dataStore.getUser());
    String localPassphrase;
    if (dataStore.getPassword() != null) {
      if (this.passphrase == null) {
        if (internalPassphrase) {
          localPassphrase = INTERNAL_PASSPHRASE;
          ini.put(dataStore.getName(), INTERNAL_PASSPHRASE_KEY, true);
        } else {
          throw new RuntimeException("A passphrase is mandatory when a password must be saved.");
        }
      } else {
        localPassphrase = this.passphrase;
      }
      String password = Protector.get(localPassphrase).encrypt(dataStore.getPassword());
      ini.put(dataStore.getName(), PASSWORD, password);
    }
    for (Map.Entry<String, String> property: dataStore.getProperties().entrySet()) {
      ini.put(dataStore.getName(), property.getKey(), property.getValue());
    }
    flush();
    return this;
  }

  public DatastoreVault save(DataStore dataStore) {

    return save(dataStore, false);
  }


  /**
   * Write the changes to the disk
   */
  private void flush() {

    try {
      getIniFile().store();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }


  private Ini getIniFile() {
    if (ini == null) {
      reload();
    }
    return ini;
  }

  /**
   * Remove all databases metadata information that matchs on of the globPatterns
   *
   * @param globPatterns
   * @return a list of database name removed
   */
  public List<String> removeDatabases(String... globPatterns) {
    Ini ini = getIniFile();
    List<String> databases = new ArrayList<>();
    for (String globPattern : globPatterns) {
      String regexpPattern = Globs.toRegexPattern(globPattern);
      for (Profile.Section section : ini.values()) {
        final String sectionDatabaseName = section.getName();
        if (sectionDatabaseName.matches(regexpPattern)) {
          Profile.Section deletedSection = ini.remove(section);
          databases.add(deletedSection.getName());
        }
      }
    }
    flush();
    return databases;
  }

  /**
   * Removes all databases
   *
   * @return
   */
  public List<String> removeAllDatabases() {
    return removeDatabases("*");
  }

  /**
   * @return all databases
   */
  public List<DataStore> getDataStores() {
    return getDataStores("*");
  }

  /**
   * @param globPatterns
   * @return all databases that match this glob patterns
   */
  public List<DataStore> getDataStores(String... globPatterns) {
    List<DataStore> dataStores = new ArrayList<>();
    for (String globPattern : globPatterns) {
      String regexpPattern = Globs.toRegexPattern(globPattern);
      dataStores.addAll(
        getIniFile().keySet()
          .stream()
          .filter(s -> s.matches(regexpPattern))
          .map(s -> getDataStore(s))
          .collect(Collectors.toList())
      );
    }
    Collections.sort(dataStores);
    return dataStores;
  }

  public List<DataStore> getDataStores(List<String> globPatterns) {
    return getDataStores(globPatterns.toArray(new String[0]));
  }

  /**
   * @param name
   * @return a database by its name or NULL
   */
  public DataStore getDataStore(String name) {
    assert name != null : "The name of the data store cannot be null";

    DataStore dataStore = null;
    Wini.Section iniSection = getIniFile().get(name);
    if (iniSection != null) {
      dataStore = DataStore.of(name);
      String connectionString = iniSection.get(URL);
      dataStore.setConnectionString(connectionString);
      dataStore.setUser(iniSection.get(USER));
      if (iniSection.get(PASSWORD) != null) {
        String localPassphrase;
        if (this.passphrase != null) {
          localPassphrase = this.passphrase;
        } else {
          final String s = iniSection.get(INTERNAL_PASSPHRASE_KEY);
          if (s != null) {
            if (s.equals("true")) {
              localPassphrase = INTERNAL_PASSPHRASE;
            } else {
              throw new RuntimeException("The internal passphrase key value (" + s + ") is unknown");
            }
          } else {
            throw new RuntimeException("The data store (" + dataStore + ") has a password. A passphrase should be provided");
          }
        }
        dataStore.setPassword(Protector.get(localPassphrase).decrypt(iniSection.get(PASSWORD)));
      }
      if (dataStore.getScheme().equals(Database.JDBC_SCHEME)) {
        dataStore = Database.of(dataStore)
          .setDriver(iniSection.get(Database.DRIVER_PROPERTY_KEY))
          .setPostConnectionStatement(iniSection.get(Database.POST_STATEMENT_PROPERTY_KEY));
      }
    } else {
      logger.warn("The database ({}) was not found. A null database was returned", name);
    }

    return dataStore;

  }

  /**
   * Reread the file
   */
  public DatastoreVault reload() {
    load();
    return this;
  }

  /**
   * Read the file
   */
  private void load() {
    try {
      if (!(Files.exists(this.path))) {
        Fs.createFile(this.path);
        ini = new Ini(this.path.toFile());

        // Add a Sqlite
        Path dbFile;
        // Trick to not have the user name in the output ie C:\Users\Username\...
        // The env value have a fake account
        final String bytle_db_databases_store = System.getenv("BYTLE_DB_SQLITE_PATH");
        if (bytle_db_databases_store != null) {
          dbFile = Paths.get(bytle_db_databases_store);
        } else {
          dbFile = Paths.get(Fs.getAppData(Tabular.APP_NAME).toAbsolutePath().toString(), Tabular.APP_NAME + ".db");
        }
        Files.createDirectories(dbFile.getParent());
        String rootWindows = "///";
        Database database = Database.of("sqlite")
          .setDriver("org.sqlite.JDBC")
          .setConnectionString("jdbc:sqlite:" + rootWindows + dbFile.toString().replace("\\", "/"));
        save(database);

        database = Database.of("oracle")
          .setDriver("oracle.jdbc.OracleDriver")
          .setConnectionString("jdbc:oracle:thin:@[host]:[port]/[servicename]");
        save(database);

        database = Database.of("sqlserver")
          .setDriver("com.microsoft.sqlserver.jdbc.SQLServerDriver")
          .setConnectionString("jdbc:sqlserver://localhost;databaseName=AdventureWorks;")
          .setUser("sa")
          .setPassword("TheSecret1!");
        save(database, true);

        database = Database.of("mysql")
          .setDriver("com.mysql.jdbc.Driver")
          .setConnectionString("jdbc:mysql://[host]:[port]/[database]");
        save(database);

        // jdbc:postgresql://host:port/database?prop=value
        database = Database.of("postgresql")
          .setDriver("org.postgresql.Driver")
          .setConnectionString("jdbc:postgresql://host:port/test?ssl=true");
        save(database);

      } else {
        ini = new Ini(this.path.toFile());
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public void removeDataStore(String name) {
    Profile.Section deletedSection = getIniFile().remove(name);
    if (deletedSection == null) {
      throw new RuntimeException("The database (" + name + ") is non existent and therefore cannot be removed.");
    }
    flush();
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
   * The difference between a {@link #getDataStore(String)} that returns NULL if it doesn't exist
   * and this function is that the {@link #getDataStore(String)} needs a good {@link #setPassphrase(String)}
   * to tell you that when the database exists to be able to decreypt and gives you the passpword.
   * <p>
   * This function doesn't need a store with a genuine passphrase to tell you if a connection exists.
   *
   * @param name
   * @return boolean
   */
  boolean exists(String name) {
    Wini.Section iniSection = getIniFile().get(name);
    if (iniSection != null) {
      return true;
    } else {
      return false;
    }
  }




  public void update(DataStore dataStore) {
    DataStore dataStoreToUpdate = this.getDataStore(dataStore.getName());
    if (dataStore.getConnectionString() !=null && !dataStore.getConnectionString().equals(dataStoreToUpdate.getConnectionString())) dataStoreToUpdate.setConnectionString(dataStore.getConnectionString());
    if (dataStore.getUser() !=null && !dataStore.getUser().equals(dataStoreToUpdate.getUser())) dataStoreToUpdate.setUser(dataStore.getUser());
    if (dataStore.getPassword() !=null && !dataStore.getPassword().equals(dataStoreToUpdate.getPassword())) dataStoreToUpdate.setPassword(dataStore.getPassword());
    if (dataStore.getScheme().equals(Database.JDBC_SCHEME)){
      Database database = (Database) dataStore;
      Database databaseToUpdate = (Database) dataStoreToUpdate;
      if (database.getDriver() !=null && !database.getDriver().equals(databaseToUpdate.getDriver())) databaseToUpdate.setDriver(database.getDriver());
      if (database.getPostConnectionStatement() !=null && !database.getPostConnectionStatement().equals(databaseToUpdate.getPostConnectionStatement())) databaseToUpdate.setPostConnectionStatement(database.getPostConnectionStatement());
      save(databaseToUpdate);
    } else {
      save(dataStoreToUpdate);
    }
  }

  public void add(DataStore dataStore) {
    assert getDataStore(dataStore.getName())==null:"The data store ("+dataStore.getName()+") exists already and cannot be added";
    if (dataStore.getConnectionString()==null){
      throw new RuntimeException("A connection string (url) is mandatory to add a datastore, the data store ("+dataStore.getName()+") does not have any.");
    }
    save(dataStore);
  }

  public DataStore getOrCreateDataStore(String name) {
    DataStore dataStore = getDataStore(name);
    if (dataStore==null){
      dataStore = DataStore.of(name);
    }
    return dataStore;
  }
}
