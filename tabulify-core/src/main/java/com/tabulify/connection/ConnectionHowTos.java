package com.tabulify.connection;

import com.tabulify.Tabular;
import com.tabulify.conf.Origin;
import net.bytle.fs.Fs;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.tabulify.connection.ConnectionAttributeEnumBase.DRIVER;

/**
 * A class that gives back default connections for how-to demos
 * See <a href="https://tabulify.com/howtos">HowTo</a>
 * <p>
 * The connection name are the name of the scheme in the URL and does not have any minus
 * because SQL see them as an operator (even without space)
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


  /**
   * @param connectionName the name of the connection
   * @param sqliteHome     where to store the database (default to tabli user home)
   * @return a JDBC connection string for the default data vault
   */
  static public String getSqliteConnectionString(String connectionName, Path sqliteHome) {

    if (sqliteHome == null) {
      sqliteHome = Tabular.TABLI_USER_HOME_PATH;
    }
    Path dbFile = sqliteHome.resolve(connectionName + ".db");
    Fs.createDirectoryIfNotExists(dbFile.getParent());
    String rootWindows = "///";
    return "jdbc:sqlite:" + rootWindows + dbFile.toString().replace("\\", "/");

  }

  /**
   * @return a map of how's datastore
   */
  static public Map<String, Connection> createHowtoConnections(Tabular tabular, Path sqliteConnectionHome) {


    Set<Connection> howToDataStoresSet = new HashSet<>();

    howToDataStoresSet.add(
      Connection.createConnectionFromProviderOrDefault(tabular, SQLITE_CONNECTION_NAME, getSqliteConnectionString(SQLITE_CONNECTION_NAME, sqliteConnectionHome))
              .addAttribute(DRIVER, "org.sqlite.JDBC", Origin.RUNTIME, tabular.getVault())
        .setDescription("The sqlite default connection")
    );

    howToDataStoresSet.add(
      Connection.createConnectionFromProviderOrDefault(tabular, SQLITE_TARGET_CONNECTION_NAME, getSqliteConnectionString(SQLITE_TARGET_CONNECTION_NAME, sqliteConnectionHome))
              .addAttribute(DRIVER, "org.sqlite.JDBC", Origin.RUNTIME, tabular.getVault())
        .setDescription("The default sqlite target (Sqlite cannot read and write with the same connection)")
    );


    howToDataStoresSet.add(
      Connection.createConnectionFromProviderOrDefault(tabular, ORACLE_CONNECTION_NAME, "jdbc:oracle:thin:@localhost:1521/freepdb1")
        .setDescription("The default oracle connection")
        .setUser("hr")
        .setPassword("oracle")
              .addAttribute(DRIVER, "oracle.jdbc.OracleDriver", Origin.RUNTIME, tabular.getVault())
    );

    howToDataStoresSet.add(
      Connection.createConnectionFromProviderOrDefault(tabular, SQLSERVER_CONNECTION_NAME, "jdbc:sqlserver://localhost:1433;databaseName=AdventureWorks;encrypt=true;trustServerCertificate=true")
        .setDescription("The default sqlserver connection")
              .addAttribute(DRIVER, "com.microsoft.sqlserver.jdbc.SQLServerDriver", Origin.RUNTIME, tabular.getVault())
        .setUser("sa")
        .setPassword("TheSecret1!")
    );

    /**
     * By default, my sql does not have any database
     * This is the one created in the test
     * {@link MySqlDataStoreResource}
     *
     * Documentation:
     * https://dev.mysql.com/doc/connector-j/8.0/en/connector-j-reference-jdbc-url-format.html
     */
    String howToDatabaseName = "howto";
    howToDataStoresSet.add(
      Connection.createConnectionFromProviderOrDefault(tabular, MYSQL_CONNECTION_NAME, "jdbc:mysql://localhost:3306/" + howToDatabaseName)
        .setDescription("The default mysql data store")
              .addAttribute(DRIVER, "com.mysql.jdbc.Driver", Origin.RUNTIME, tabular.getVault())
        .setUser("root")
        .setPassword("my-secret-pw") // from the docker doc - https://hub.docker.com/_/mysql
    );

    // jdbc:postgresql://host:port/database?prop=value
    howToDataStoresSet.add(
      Connection.createConnectionFromProviderOrDefault(tabular, POSTGRESQL_CONNECTION_NAME, "jdbc:postgresql://localhost:5432/postgres")
        .setDescription("The default postgres data store")
              .addAttribute(DRIVER, "org.postgresql.Driver", Origin.RUNTIME, tabular.getVault())
        .setUser("postgres")
        .setPassword("welcome")
    );


    return howToDataStoresSet
      .stream()
      .collect(Collectors.toMap(Connection::getName, Connection::of));


  }

  /**
   * @return the location of the how-to files
   * This function takes care of the fact that the code may run
   * in a development setting or in a distribution one
   */
  protected static Path getHowToFilesPath(Tabular tabular) {

    /*
     * Dev environment
     */
    if (tabular.isIdeEnv()) {
      Path path;
      path = tabular.getHomePath()
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
    return tabular.getHomePath()
      .resolve(DIST_RESOURCE_DIRECTORY)
      .resolve(ConnectionBuiltIn.HOW_TO_FILE_CONNECTION_NAME)
      .toAbsolutePath()
      .normalize();

  }

  /**
   * This function takes care of the fact that the code may run
   * in a development setting or in a distribution one
   */
  protected static Path getEntitiesRootPath(Tabular tabular) {

    /*
     * Dev environment
     */
    if (tabular.isIdeEnv()) {
      Path path = tabular.getHomePath()
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
    return tabular.getHomePath()
      .resolve(DIST_RESOURCE_DIRECTORY)
      .resolve(ConnectionBuiltIn.ENTITY_CONNECTION_NAME)
      .toAbsolutePath()
      .normalize();
  }

  /**
   * This function takes care of the fact that the code may run
   * in a development setting or in a distribution one
   */
  protected static Path getTpcDsQueriesPath(Tabular tabular) {

    /**
     * Dev environment
     */
    if (tabular.isIdeEnv()) {
      Path path = tabular.getHomePath()
        .resolve("tabulify-jdbc")
        .resolve("src")
        .resolve("main")
        .resolve("sql")
        .resolve("tpcds")
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
    return tabular.getHomePath()
      .resolve(DIST_RESOURCE_DIRECTORY)
      .resolve(ConnectionBuiltIn.TPCDS_QUERY_CONNECTION_NAME)
      .toAbsolutePath()
      .normalize();
  }

}
