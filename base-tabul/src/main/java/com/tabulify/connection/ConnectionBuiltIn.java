package com.tabulify.connection;

import com.tabulify.Tabular;
import com.tabulify.memory.MemoryConnectionProvider;
import com.tabulify.noop.NoopConnectionProvider;
import com.tabulify.fs.Xdg;
import com.tabulify.type.KeyNormalizer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * The internal connection
 */
public class ConnectionBuiltIn {

  public static final KeyNormalizer APP_CONNECTION = KeyNormalizer.createSafe("app");
  public static final KeyNormalizer MEMORY_CONNECTION = KeyNormalizer.createSafe("memory");
  public static final KeyNormalizer TPCDS_CONNECTION = KeyNormalizer.createSafe("tpcds");
  public static final KeyNormalizer NO_OP_CONNECTION = KeyNormalizer.createSafe("noop");

  /**
   * The current directory (was known as `local`)
   */
  public static final KeyNormalizer CD_LOCAL_FILE_SYSTEM = KeyNormalizer.createSafe("cd");
  public static final KeyNormalizer MD_LOCAL_FILE_SYSTEM = KeyNormalizer.createSafe("md");
  // tmp, no temp, we win one letter,
  // and it's the temporary directory name on linux
  public static final KeyNormalizer TEMP_LOCAL_FILE_SYSTEM = KeyNormalizer.createSafe("tmp");
  public static final KeyNormalizer HOME_LOCAL_FILE_SYSTEM = KeyNormalizer.createSafe("home");
  // XDG_DATA_HOME (where to store data by default such as data path in error)
  public static final KeyNormalizer DATA_HOME_LOCAL_FILE_SYSTEM = KeyNormalizer.createSafe("data-home");
  public static final KeyNormalizer LOG_LOCAL_CONNECTION = KeyNormalizer.createSafe("log");
  public static final KeyNormalizer DESKTOP_LOCAL_FILE_SYSTEM = KeyNormalizer.createSafe("desktop");
  /**
   * The connections that points to the location of the internal files
   * howto files, entity
   */
  public static final KeyNormalizer HOW_TO_FILE_CONNECTION_NAME = KeyNormalizer.createSafe("howto");
  public static final KeyNormalizer ENTITY_CONNECTION_NAME = KeyNormalizer.createSafe("entity");
  public static final KeyNormalizer TPCDS_QUERY_CONNECTION_NAME = KeyNormalizer.createSafe("tpcds_query");
  /**
   * The base directory in the distribution where we will find all resources (entity, howto, sql, ...)
   */
  private static final String DISTRIBUTION_RESOURCES_DIRECTORY = "resources";

  /**
   * Create the built-in, internal connections
   *
   * @param tabulUserHome         - the tabul user home
   * @param osUserHome            - the os user home
   * @param tabulInstallationHome - the installation home
   */
  public static Map<KeyNormalizer, Connection> loadBuiltInConnections(Tabular tabular, Path tabulUserHome, Path osUserHome, Path tabulInstallationHome) {

    Map<KeyNormalizer, Connection> connectionList = new HashMap<>();

    /**
     * Internal data stores
     * Not in a static field, please
     * Because of test, we need different scope
     * which is the goal of a tabular
     */
    // Local Fs
    String localFileUrl = Paths.get(".")
      .toAbsolutePath()
      .normalize()
      .toUri()
      .toString();
    Connection localConnection = Connection
      .createConnectionFromProviderOrDefault(tabular, ConnectionBuiltIn.CD_LOCAL_FILE_SYSTEM, localFileUrl)
      .setComment("The local file system")
      .setOrigin(ObjectOrigin.BUILT_IN);
    connectionList.put(localConnection.getName(), localConnection);


    // Local runtime temporary Directory
    String localTempUrl = Xdg.getRuntimeTemporaryDir(tabular.getApplicationName())
      .toAbsolutePath()
      .normalize()
      .toUri()
      .toString();
    Connection temp = Connection.createConnectionFromProviderOrDefault(tabular, ConnectionBuiltIn.TEMP_LOCAL_FILE_SYSTEM, localTempUrl)
      .setComment("The temporary runtime directory")
      .setOrigin(ObjectOrigin.BUILT_IN);
    connectionList.put(temp.getName(), temp);

    // User Home
    String localUserUrl = osUserHome
      .toAbsolutePath()
      .normalize()
      .toUri()
      .toString();
    Connection user = Connection.createConnectionFromProviderOrDefault(tabular, ConnectionBuiltIn.HOME_LOCAL_FILE_SYSTEM, localUserUrl)
      .setComment("The user home directory of the local file system")
      .setOrigin(ObjectOrigin.BUILT_IN);
    connectionList.put(user.getName(), user);

    // Data Home
    String dataHomeUrl = Xdg.getDataHome(tabular.getApplicationName()).toAbsolutePath().normalize().toUri().toString();
    Connection dataHome = Connection.createConnectionFromProviderOrDefault(tabular, ConnectionBuiltIn.DATA_HOME_LOCAL_FILE_SYSTEM, dataHomeUrl)
      .setComment("The data home directory (Default storage for data)")
      .setOrigin(ObjectOrigin.BUILT_IN);
    connectionList.put(dataHome.getName(), dataHome);

    /**
     * Logs
     */
    String localLogsUriString = tabulUserHome.resolve("logs").toUri().toString();
    Connection logs = Connection.createConnectionFromProviderOrDefault(tabular, ConnectionBuiltIn.LOG_LOCAL_CONNECTION, localLogsUriString)
      .setComment("The tabul logs")
      .setOrigin(ObjectOrigin.BUILT_IN);
    connectionList.put(logs.getName(), logs);

    String localDesktopUrl = osUserHome
      .resolve("Desktop")
      .toAbsolutePath()
      .normalize()
      .toUri()
      .toString();
    Connection desktop = Connection.createConnectionFromProviderOrDefault(tabular, ConnectionBuiltIn.DESKTOP_LOCAL_FILE_SYSTEM, localDesktopUrl)
      .setComment("The user desktop directory of the local file system")
      .setOrigin(ObjectOrigin.BUILT_IN);
    connectionList.put(desktop.getName(), desktop);

    // Memory
    Connection memoryConnection = Connection.createConnectionFromProviderOrDefault(tabular, ConnectionBuiltIn.MEMORY_CONNECTION, MemoryConnectionProvider.URI)
      .setOrigin(ObjectOrigin.BUILT_IN);
    connectionList.put(memoryConnection.getName(), memoryConnection);

    tabular.setDefaultConnection(memoryConnection);

    // TpcsDs
    Connection tpcDs = Connection.createConnectionFromProviderOrDefault(tabular, ConnectionBuiltIn.TPCDS_CONNECTION, ConnectionBuiltIn.TPCDS_CONNECTION.toCliLongOptionName())
      .setOrigin(ObjectOrigin.BUILT_IN);
    connectionList.put(tpcDs.getName(), tpcDs);

    // NoOp
    Connection noOp = Connection.createConnectionFromProviderOrDefault(tabular, ConnectionBuiltIn.NO_OP_CONNECTION, NoopConnectionProvider.NOOP_SCHEME)
      .setOrigin(ObjectOrigin.BUILT_IN);
    connectionList.put(noOp.getName(), noOp);


    // The how-to-files
    Path howToFilesPath = getHowToFilesPath(tabular, tabulInstallationHome);
    Connection howtoFiles = Connection.createConnectionFromProviderOrDefault(tabular, HOW_TO_FILE_CONNECTION_NAME, howToFilesPath.toUri().toString())
      .setComment("The location of the how to files");
    connectionList.put(howtoFiles.getName(), howtoFiles);

    // The entities
    Path entityRootPath = getEntitiesRootPath(tabular, tabulInstallationHome);
    Connection entityFiles = Connection.createConnectionFromProviderOrDefault(tabular, ENTITY_CONNECTION_NAME, entityRootPath.toUri().toString())
      .setComment("The location of the entity files");
    connectionList.put(entityFiles.getName(), entityFiles);

    Path tpcDsQueriesPath = getTpcDsQueriesPath(tabular, tabulInstallationHome);
    Connection tpcdsQuery = Connection.createConnectionFromProviderOrDefault(tabular, TPCDS_QUERY_CONNECTION_NAME, tpcDsQueriesPath.toUri().toString())
      .setComment("The location of the Tpc Ds queries");
    connectionList.put(tpcdsQuery.getName(), tpcdsQuery);

    return connectionList;

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
        .resolve("cli-tabul")
        .resolve("src")
        .resolve("main")
        .resolve(HOW_TO_FILE_CONNECTION_NAME.toFileCase())
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
      .resolve(DISTRIBUTION_RESOURCES_DIRECTORY)
      .resolve(HOW_TO_FILE_CONNECTION_NAME.toFileCase())
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
        .resolve("data-generation-entities")
        .resolve("src")
        .resolve("main")
        .resolve("resources")
        .resolve(ENTITY_CONNECTION_NAME.toFileCase())
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
      .resolve(DISTRIBUTION_RESOURCES_DIRECTORY)
      .resolve(ENTITY_CONNECTION_NAME.toFileCase())
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
        .resolve("jdbc-tabul")
        .resolve("src")
        .resolve("main")
        .resolve("sql")
        .resolve(TPCDS_QUERY_CONNECTION_NAME.toFileCase())
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
      .resolve(DISTRIBUTION_RESOURCES_DIRECTORY)
      .resolve(TPCDS_QUERY_CONNECTION_NAME.toFileCase())
      .toAbsolutePath()
      .normalize();
  }
}
