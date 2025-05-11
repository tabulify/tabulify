package com.tabulify.connection;

import com.tabulify.Tabular;
import com.tabulify.conf.Origin;
import net.bytle.exception.InternalException;
import net.bytle.fs.Fs;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static com.tabulify.connection.ConnectionAttributeEnumBase.DRIVER;

/**
 * A class that gives back default connections for how-to demos
 * See <a href="https://tabulify.com/howtos">HowTo</a>
 * <p>
 * The connection name are the name of the scheme in the URL and does not have any minus
 * because SQL see them as an operator (even without space)
 * <p></p>
 * We build them one by one because it's more handy when we debug
 */
public class ConnectionHowTos {

  /**
   * The name of the resource directory in the distribution
   * ALl resources such as howto file, entity, sql should be below this directory
   */
  public static final String DIST_RESOURCE_DIRECTORY = "resources";


  /**
   * The sqlite datastore name
   * Another sqlite datastore is needed for Sqlite
   * because you can't read and write at the same time
   */
  public static final String SQLITE_CONNECTION_NAME = "sqlite";
  public static final String SQLITE_TARGET_CONNECTION_NAME = "sqlite_target";

  /**
   * The other relational database
   */
  public static final String ORACLE_CONNECTION_NAME = "oracle";
  public static final String SQLSERVER_CONNECTION_NAME = "sqlserver";
  public static final String MYSQL_CONNECTION_NAME = "mysql";
  public static final String POSTGRESQL_CONNECTION_NAME = "postgres"; // The official docker name, shorter
  static final Set<String> allHowtoNames = Set.of(
    SQLITE_CONNECTION_NAME,
    SQLITE_TARGET_CONNECTION_NAME,
    ORACLE_CONNECTION_NAME,
    SQLSERVER_CONNECTION_NAME,
    MYSQL_CONNECTION_NAME,
    POSTGRESQL_CONNECTION_NAME
  );
  private final Tabular tabular;
  private final Path userHomePath;
  final Map<String, Connection> howToDataStoresSet = new HashMap<>();

  public ConnectionHowTos(Tabular tabular, Path tabliUserHomePath) {
    this.tabular = tabular;
    this.userHomePath = tabliUserHomePath;
  }


  /**
   * @param connectionName the name of the connection
   * @param userHomePath   where to store the database (default to tabli user home)
   * @return a JDBC connection string for the default data vault
   */
  static public String getSqliteConnectionString(String connectionName, Path userHomePath) {

    assert userHomePath != null;
    Path dbFile = userHomePath.resolve(connectionName + ".db");
    Fs.createDirectoryIfNotExists(dbFile.getParent());
    String rootWindows = "///";
    return "jdbc:sqlite:" + rootWindows + dbFile.toString().replace("\\", "/");

  }

  /**
   * @return a map of how's datastore
   */
  static public ConnectionHowTos createHowtoConnections(Tabular tabular, Path tabliUserHomePath) {
    return new ConnectionHowTos(tabular, tabliUserHomePath);


  }

  /**
   * @return the location of the how-to files
   * This function takes care of the fact that the code may run
   * in a development setting or in a distribution one
   */
  protected static Path getHowToFilesPath(Tabular tabular, Path installationHomePath) {

    /*
     * Dev environment
     */
    if (tabular.isIdeEnv()) {
      Path path;
      path = installationHomePath
        .resolve("tabulify-cli")
        .resolve("src")
        .resolve("main")
        .resolve(ConnectionBuiltIn.HOW_TO_FILE_CONNECTION_NAME)
        .normalize();

      if (!Files.exists(path)) {
        throw new RuntimeException("The howto files directory path (" + path + ") does not exist for a dev env. Have they moved ?");
      }
      return path;

    }

    /*
     * Distribution
     */
    return installationHomePath
      .resolve(DIST_RESOURCE_DIRECTORY)
      .resolve(ConnectionBuiltIn.HOW_TO_FILE_CONNECTION_NAME)
      .toAbsolutePath()
      .normalize();

  }

  /**
   * This function takes care of the fact that the code may run
   * in a development setting or in a distribution one
   */
  protected static Path getEntitiesRootPath(Tabular tabular, Path tabliInstallationHome) {

    /*
     * Dev environment
     */
    if (tabular.isIdeEnv()) {
      Path path = tabliInstallationHome
        .resolve("tabulify-gen-entities")
        .resolve("src")
        .resolve("main")
        .resolve("resources")
        .resolve(ConnectionBuiltIn.ENTITY_CONNECTION_NAME)
        .normalize();

      if (!Files.exists(path)) {
        throw new RuntimeException("The entity directory path (" + path + ") does not exist for a dev env. Have they moved ?");
      } else {
        return path;
      }
    }

    /**
     * Distribution
     */
    return tabliInstallationHome
      .resolve(DIST_RESOURCE_DIRECTORY)
      .resolve(ConnectionBuiltIn.ENTITY_CONNECTION_NAME)
      .toAbsolutePath()
      .normalize();
  }

  /**
   * This function takes care of the fact that the code may run
   * in a development setting or in a distribution one
   */
  protected static Path getTpcDsQueriesPath(Tabular tabular, Path tabliInstallationHome) {

    /**
     * Dev environment
     */
    if (tabular.isIdeEnv()) {
      Path path = tabliInstallationHome
        .resolve("tabulify-jdbc")
        .resolve("src")
        .resolve("main")
        .resolve("sql")
        .resolve(ConnectionBuiltIn.TPCDS_QUERY_CONNECTION_NAME)
        .normalize();
      if (!Files.exists(path)) {
        throw new RuntimeException("The tpcds directory path (" + path + ") does not exist for a dev env. Have they moved ?");
      } else {
        return path;
      }
    }

    /**
     * Distribution
     */
    return tabliInstallationHome
      .resolve(DIST_RESOURCE_DIRECTORY)
      .resolve(ConnectionBuiltIn.TPCDS_QUERY_CONNECTION_NAME)
      .toAbsolutePath()
      .normalize();
  }

  public Map<String, ? extends Connection> getAll() {

    for (String name : allHowtoNames) {
      if (!howToDataStoresSet.containsKey(name)) {
        howToDataStoresSet.put(name, this.buildConnection(name));
      }
    }
    return howToDataStoresSet;
  }

  public Connection get(String connectionName) {
    return this.howToDataStoresSet.computeIfAbsent(connectionName, this::buildConnection);
  }

  private Connection buildConnection(String connectionName) {
    switch (connectionName) {
      case SQLITE_CONNECTION_NAME:
        return Connection.createConnectionFromProviderOrDefault(tabular, SQLITE_CONNECTION_NAME, getSqliteConnectionString(SQLITE_CONNECTION_NAME, userHomePath))
          .addAttribute(DRIVER, "org.sqlite.JDBC", Origin.DEFAULT, tabular.getVault())
          .setComment("The sqlite default connection");
      case SQLITE_TARGET_CONNECTION_NAME:
        return Connection.createConnectionFromProviderOrDefault(tabular, SQLITE_TARGET_CONNECTION_NAME, getSqliteConnectionString(SQLITE_TARGET_CONNECTION_NAME, userHomePath))
          .addAttribute(DRIVER, "org.sqlite.JDBC", Origin.DEFAULT, tabular.getVault())
          .setComment("The default sqlite target (Sqlite cannot read and write with the same connection)");
      case ORACLE_CONNECTION_NAME:
        return Connection.createConnectionFromProviderOrDefault(tabular, ORACLE_CONNECTION_NAME, "jdbc:oracle:thin:@localhost:1521/freepdb1")
          .setComment("The default oracle connection")
          .setUser("hr")
          .setPassword("oracle")
          .addAttribute(DRIVER, "oracle.jdbc.OracleDriver", Origin.DEFAULT, tabular.getVault());
      case SQLSERVER_CONNECTION_NAME:
        return Connection.createConnectionFromProviderOrDefault(tabular, SQLSERVER_CONNECTION_NAME, "jdbc:sqlserver://localhost:1433;databaseName=AdventureWorks;encrypt=true;trustServerCertificate=true")
          .setComment("The default sqlserver connection")
          .addAttribute(DRIVER, "com.microsoft.sqlserver.jdbc.SQLServerDriver", Origin.DEFAULT, tabular.getVault())
          .setUser("sa")
          .setPassword("TheSecret1!");
      case MYSQL_CONNECTION_NAME:
        /**
         * By default, my sql does not have any database
         * This is the one created in the test
         * {@link MySqlDataStoreResource}
         *
         * Documentation:
         * https://dev.mysql.com/doc/connector-j/8.0/en/connector-j-reference-jdbc-url-format.html
         */
        String howToDatabaseName = "howto";
        return Connection.createConnectionFromProviderOrDefault(tabular, MYSQL_CONNECTION_NAME, "jdbc:mysql://localhost:3306/" + howToDatabaseName)
          .setComment("The default mysql data store")
          .addAttribute(DRIVER, "com.mysql.cj.jdbc.Driver", Origin.DEFAULT, tabular.getVault())
          .setUser("root")
          .setPassword("my-secret-pw"); // from the docker doc - https://hub.docker.com/_/mysql
      case POSTGRESQL_CONNECTION_NAME:
        // jdbc:postgresql://host:port/database?prop=value
        return Connection.createConnectionFromProviderOrDefault(tabular, POSTGRESQL_CONNECTION_NAME, "jdbc:postgresql://localhost:5432/postgres")
          .setComment("The default postgres data store")
          .addAttribute(DRIVER, "org.postgresql.Driver", Origin.DEFAULT, tabular.getVault())
          .setUser("postgres")
          .setPassword("welcome");
      default:
        throw new InternalException("The connection " + connectionName + " is not an howto connection");
    }
  }
}
