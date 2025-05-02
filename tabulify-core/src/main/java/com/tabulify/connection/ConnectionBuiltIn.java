package com.tabulify.connection;

import com.tabulify.Tabular;
import com.tabulify.conf.Origin;
import com.tabulify.memory.MemoryConnectionProvider;
import com.tabulify.noop.NoopConnectionProvider;
import net.bytle.exception.IllegalStructure;
import net.bytle.fs.Fs;
import net.bytle.type.KeyNormalizer;
import net.bytle.type.UriEnhanced;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * The internal connection
 */
public class ConnectionBuiltIn {

  public static final String PROJECT_CONNECTION = "project";
  public static final String MEMORY_CONNECTION = "memory";
  public static final String TPCDS_CONNECTION = "tpcds";
  public static final String NO_OP_CONNECTION = "noop";
  /**
   * We choose smtp and not email
   * because email is a more common word
   * and may clash if a user uses it as connection name
   */
  public static final String SMTP_CONNECTION = "smtp";
  public static final String CD_LOCAL_FILE_SYSTEM = "cd";
  public static final String SD_LOCAL_FILE_SYSTEM = "sd";
  public static final String TEMP_LOCAL_FILE_SYSTEM = "temp";
  public static final String HOME_LOCAL_FILE_SYSTEM = "home";
  public static final String LOG_LOCAL_CONNECTION = "log";
  public static final String DESKTOP_LOCAL_FILE_SYSTEM = "desktop";
  /**
   * The connections that points to the location of the internal files
   * howto files, entity
   */
  public static final String HOW_TO_FILE_CONNECTION_NAME = "howto";
  public static final String ENTITY_CONNECTION_NAME = "entity";
  public static final String TPCDS_QUERY_CONNECTION_NAME = "tpcds_query";

  /**
   * Create the built-in, internal connections
   */
  public static void loadBuiltInConnections(Tabular tabular) {

    /**
     * Internal datastores
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
      .setDescription("The local file system")
      .setOrigin(ConnectionOrigin.BUILT_IN);
    tabular.addConnection(localConnection);


    // Local temporary Directory
    String localTempUrl = Fs.getTempDirectory()
      .toAbsolutePath()
      .normalize()
      .toUri()
      .toString();
    Connection temp = Connection.createConnectionFromProviderOrDefault(tabular, ConnectionBuiltIn.TEMP_LOCAL_FILE_SYSTEM, localTempUrl)
      .setDescription("The local temporary directory of the local file system")
      .setOrigin(ConnectionOrigin.BUILT_IN);
    tabular.addConnection(temp);
    // Local temporary Directory

    String localUserUrl = Fs.getUserHome()
      .toAbsolutePath()
      .normalize()
      .toUri()
      .toString();
    Connection user = Connection.createConnectionFromProviderOrDefault(tabular, ConnectionBuiltIn.HOME_LOCAL_FILE_SYSTEM, localUserUrl)
      .setDescription("The user home directory of the local file system")
      .setOrigin(ConnectionOrigin.BUILT_IN);
    tabular.addConnection(user);

    String localLogsUriString = ConnectionHowTos.getSqliteConnectionString(tabular, ConnectionBuiltIn.LOG_LOCAL_CONNECTION);
    Connection logs = Connection.createConnectionFromProviderOrDefault(tabular, ConnectionBuiltIn.LOG_LOCAL_CONNECTION, localLogsUriString)
      .setDescription("The tabli logs")
      .setOrigin(ConnectionOrigin.BUILT_IN);
    tabular.addConnection(logs);

    String localDesktopUrl = Fs.getUserDesktop()
      .toAbsolutePath()
      .normalize()
      .toUri()
      .toString();
    Connection desktop = Connection.createConnectionFromProviderOrDefault(tabular, ConnectionBuiltIn.DESKTOP_LOCAL_FILE_SYSTEM, localDesktopUrl)
      .setDescription("The user desktop directory of the local file system")
      .setOrigin(ConnectionOrigin.BUILT_IN);
    tabular.addConnection(desktop);

    // Memory
    Connection memoryConnection = Connection.createConnectionFromProviderOrDefault(tabular, ConnectionBuiltIn.MEMORY_CONNECTION, MemoryConnectionProvider.SCHEME)
      .setOrigin(ConnectionOrigin.BUILT_IN);
    tabular.addConnection(memoryConnection);

    tabular.setDefaultConnection(memoryConnection);

    // TpcsDs
    Connection tpcDs = Connection.createConnectionFromProviderOrDefault(tabular, ConnectionBuiltIn.TPCDS_CONNECTION, ConnectionBuiltIn.TPCDS_CONNECTION)
      .setOrigin(ConnectionOrigin.BUILT_IN)
      .addAttribute(KeyNormalizer.create("scale"), 0.01, Origin.RUNTIME);
    tabular.addConnection(tpcDs);

    // NoOp
    Connection noOp = Connection.createConnectionFromProviderOrDefault(tabular, ConnectionBuiltIn.NO_OP_CONNECTION, NoopConnectionProvider.NOOP_SCHEME)
      .setOrigin(ConnectionOrigin.BUILT_IN);
    tabular.addConnection(noOp);

    // Email
    UriEnhanced emailUri;
    try {
      emailUri = UriEnhanced.create()
        .setScheme("smtp")
        .setHost("localhost");
    } catch (IllegalStructure e) {
      throw new RuntimeException(e);
    }
    Connection smtpConnection = Connection.createConnectionFromProviderOrDefault(tabular, ConnectionBuiltIn.SMTP_CONNECTION, emailUri.toUri().toString())
      .setDescription("Smtp")
      .setOrigin(ConnectionOrigin.BUILT_IN);
    tabular.addConnection(smtpConnection);

    // The how-to-files
    Path howToFilesPath = ConnectionHowTos.getHowToFilesPath(tabular);
    tabular.addConnection(
      Connection.createConnectionFromProviderOrDefault(tabular, HOW_TO_FILE_CONNECTION_NAME, howToFilesPath.toUri().toString())
        .setDescription("The location of the how to files")
    );

    // The entities
    Path entityRootPath = ConnectionHowTos.getEntitiesRootPath(tabular);
    tabular.addConnection(
      Connection.createConnectionFromProviderOrDefault(tabular, ENTITY_CONNECTION_NAME, entityRootPath.toUri().toString())
        .setDescription("The location of the entity files")
    );

    Path tpcDsQueriesPath = ConnectionHowTos.getTpcDsQueriesPath(tabular);
    tabular.addConnection(
      Connection.createConnectionFromProviderOrDefault(tabular, TPCDS_QUERY_CONNECTION_NAME, tpcDsQueriesPath.toUri().toString())
        .setDescription("The location of the Tpc Ds queries")
    );

  }

}
