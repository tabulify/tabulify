package com.tabulify.connection;

import com.tabulify.TabularAttributes;
import com.tabulify.Tabular;
import net.bytle.fs.Fs;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
   * The connections that points to the location of the internal files
   * howto files, entity
   */
  public static final String HOW_TO_FILE_CONNECTION_NAME = "howto";
  public static final String ENTITY_CONNECTION_NAME = "entity";
  public static final String TPCDS_QUERY_CONNECTION_NAME = "tpcds_query";

  /**
   * @param connectionName the name of the connection
   * @return a JDBC connection string for the default data vault
   */
  static private String getSqliteConnectionString(String connectionName) {

    Path dbFile;
    // Trick to not have the username in the output ie C:\Users\Username\...
    // The env value have a fake account
    final String bytle_db_databases_store = System.getenv("BYTLE_DB_SQLITE_PATH");
    if (bytle_db_databases_store != null) {
      dbFile = Paths.get(bytle_db_databases_store);
    } else {
      dbFile = Fs.getUserAppData(TabularAttributes.APP_NAME.getDefaultValue().toString()).resolve(connectionName + ".db");
    }
    Fs.createDirectoryIfNotExists(dbFile.getParent());
    String rootWindows = "///";
    return "jdbc:sqlite:" + rootWindows + dbFile.toString().replace("\\", "/");

  }

  /**
   * @return a map of how's datastore
   */
  static public Map<String, Connection> createHowtoConnections(Tabular tabular) {


      Set<Connection> howToDataStoresSet = new HashSet<>();

      howToDataStoresSet.add(
        Connection.createConnectionFromProviderOrDefault(tabular, SQLITE_CONNECTION_NAME, getSqliteConnectionString(SQLITE_CONNECTION_NAME))
          .addVariable("Driver", "org.sqlite.JDBC")
          .setDescription("The sqlite default connection")
      );

      howToDataStoresSet.add(
        Connection.createConnectionFromProviderOrDefault(tabular, SQLITE_TARGET_CONNECTION_NAME, getSqliteConnectionString(SQLITE_TARGET_CONNECTION_NAME))
          .addVariable("Driver", "org.sqlite.JDBC")
          .setDescription("The default sqlite target (Sqlite cannot read and write with the same connection)")
      );


      howToDataStoresSet.add(
        Connection.createConnectionFromProviderOrDefault(tabular, ORACLE_CONNECTION_NAME, "jdbc:oracle:thin:@localhost:1521:xe")
          .setDescription("The default oracle connection")
          .setPassword("oracle")
          .setUser("system")
          .addVariable("Driver", "oracle.jdbc.OracleDriver")
      );

      howToDataStoresSet.add(
        Connection.createConnectionFromProviderOrDefault(tabular, SQLSERVER_CONNECTION_NAME, "jdbc:sqlserver://localhost;databaseName=AdventureWorks;")
          .setDescription("The default sqlserver connection")
          .addVariable("Driver", "com.microsoft.sqlserver.jdbc.SQLServerDriver")
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
          .addVariable("Driver", "com.mysql.jdbc.Driver")
          .setUser("root")
          .setPassword(tabular.getVault().encrypt("my-secret-pw")) // from the docker doc - https://hub.docker.com/_/mysql
      );

      // jdbc:postgresql://host:port/database?prop=value
      howToDataStoresSet.add(
        Connection.createConnectionFromProviderOrDefault(tabular, POSTGRESQL_CONNECTION_NAME, "jdbc:postgresql://localhost:5432/postgres")
          .setDescription("The default postgres data store")
          .addVariable("Driver", "org.postgresql.Driver")
          .setUser("postgres")
          .setPassword(tabular.getVault().encrypt("welcome"))
      );

      // The how-to-files
      Path howToFilesPath = ConnectionHowTos.getHowToFilesPath(tabular);
      howToDataStoresSet.add(
        Connection.createConnectionFromProviderOrDefault(tabular, HOW_TO_FILE_CONNECTION_NAME, howToFilesPath.toUri().toString())
          .setDescription("The local location of the how to files")
      );

      // The entities
      Path entityRootPath = ConnectionHowTos.getEntitiesRootPath(tabular);
      howToDataStoresSet.add(
        Connection.createConnectionFromProviderOrDefault(tabular, ENTITY_CONNECTION_NAME, entityRootPath.toUri().toString())
          .setDescription("The location of the entity files")
      );

      Path tpcDsQueriesPath = ConnectionHowTos.getTpcDsQueriesPath(tabular);
      howToDataStoresSet.add(
        Connection.createConnectionFromProviderOrDefault(tabular, TPCDS_QUERY_CONNECTION_NAME, tpcDsQueriesPath.toUri().toString())
          .setDescription("The location of the Tpc Ds queries")
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
    if (tabular.isDev()) {
      Path path;
      path = tabular.getHomePath()
        .resolve("tabulify-cli")
        .resolve("src")
        .resolve("main")
        .resolve(HOW_TO_FILE_CONNECTION_NAME)
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
      .resolve(HOW_TO_FILE_CONNECTION_NAME)
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
    if (tabular.isDev()) {
      Path path = tabular.getHomePath()
          .resolve("tabulify-gen-entities")
          .resolve("src")
          .resolve("main")
          .resolve("resources")
          .resolve(ENTITY_CONNECTION_NAME)
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
      .resolve(ENTITY_CONNECTION_NAME)
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
    if (tabular.isDev()) {
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
      .resolve(TPCDS_QUERY_CONNECTION_NAME)
      .toAbsolutePath()
      .normalize();
  }

}
