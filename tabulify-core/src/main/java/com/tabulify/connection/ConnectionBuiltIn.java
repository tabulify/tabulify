package com.tabulify.connection;

import com.tabulify.Tabular;
import com.tabulify.TabularAttributes;
import com.tabulify.Vault;
import com.tabulify.memory.MemoryConnectionProvider;
import com.tabulify.noop.NoopConnectionProvider;
import net.bytle.exception.CastException;
import net.bytle.exception.IllegalArgumentExceptions;
import net.bytle.exception.IllegalStructure;
import net.bytle.exception.NoVariableException;
import net.bytle.fs.Fs;
import net.bytle.os.Oss;
import net.bytle.type.Integers;
import net.bytle.type.UriEnhanced;
import net.bytle.type.Variable;

import java.net.UnknownHostException;
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
     * Connection (should be created after the {@link Vault})
     * Loaded by order of precedence
     */
    if (tabular.getProjectConfigurationFile() != null) {
      String localFileUrl = tabular.getProjectConfigurationFile()
        .getProjectDirectory()
        .toAbsolutePath()
        .normalize()
        .toUri()
        .toString();
      tabular.createRuntimeConnection(ConnectionBuiltIn.PROJECT_CONNECTION, localFileUrl)
        .setDescription("The project home directory");
    }

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

    Path logDbPath = Fs.getUserAppData(TabularAttributes.APP_NAME.toString()).resolve(ConnectionBuiltIn.LOG_LOCAL_CONNECTION + ".db");
    String rootWindows = "///";
    String localLogsUriString = "jdbc:sqlite:" + rootWindows + logDbPath.toString().replace("\\", "/");
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
      .addVariable("scale", 0.01);
    tabular.addConnection(tpcDs);

    // NoOp
    Connection noOp = Connection.createConnectionFromProviderOrDefault(tabular, ConnectionBuiltIn.NO_OP_CONNECTION, NoopConnectionProvider.NOOP_SCHEME)
      .setOrigin(ConnectionOrigin.BUILT_IN);
    tabular.addConnection(noOp);

    // Email
    UriEnhanced emailUri = UriEnhanced.create()
      .setScheme("smtp");

    String smtpHostKey = "SMTP_HOST";
    String smtpHost = tabular.getVariableAsStringOrDefault(smtpHostKey, "localhost");
    try {
      emailUri.setHost(smtpHost);
    } catch (IllegalStructure e) {
      throw IllegalArgumentExceptions.createFromMessage("The environment variable (" + smtpHostKey + ") has a invalid value (" + smtpHost + "). Error: " + e.getMessage(), e);
    }
    String smtpPortEnv = "SMTP_PORT";
    String smtpPort = tabular.getVariableAsStringOrDefault(smtpPortEnv, "25");
    try {
      emailUri.setPort(Integers.createFromObject(smtpPort).toInteger());
    } catch (CastException e) {
      throw IllegalArgumentExceptions.createFromMessage("The environment variable (" + smtpPortEnv + ") has a invalid value (" + smtpPort + "). Error: " + e.getMessage(), e);
    }

    String smtpFrom = tabular.getVariableAsStringOrDefault("SMTP_FROM", null);
    if (smtpFrom != null) {
      emailUri.addQueryProperty("from", smtpFrom);
    } else {
      try {
        emailUri.addQueryProperty("from", Oss.getUser() + "@" + Oss.getFqdn().toStringWithoutRoot());
      } catch (UnknownHostException e) {
        // oeps
      }
    }
    String smtpFromName = tabular.getVariableAsStringOrDefault("SMTP_FROM_NAME", null);
    if (smtpFromName != null) {
      emailUri.addQueryProperty("from-name", smtpFrom);
    }
    String smtpTo = tabular.getVariableAsStringOrDefault("SMTP_TO", null);
    if (smtpTo != null) {
      emailUri.addQueryProperty("to", smtpTo);
    }
    String smtpToNames = tabular.getVariableAsStringOrDefault("SMTP_TO_NAMES", null);
    if (smtpToNames != null) {
      emailUri.addQueryProperty("to-names", smtpTo);
    }
    String smtpAuth = tabular.getVariableAsStringOrDefault("SMTP_AUTH", null);
    if (smtpAuth != null) {
      emailUri.addQueryProperty("auth", smtpAuth);
    }
    String smtpTls = tabular.getVariableAsStringOrDefault("SMTP_TLS", null);
    if (smtpTls != null) {
      emailUri.addQueryProperty("tls", smtpTls);
    }

    Connection smtpConnection = Connection.createConnectionFromProviderOrDefault(tabular, ConnectionBuiltIn.SMTP_CONNECTION, emailUri.toUri().toString())
      .setOrigin(ConnectionOrigin.BUILT_IN);
    String smtpUser = tabular.getVariableAsStringOrDefault("SMTP_USER", null);
    if (smtpUser != null) {
      smtpConnection.setUser(smtpUser);
    }

    try {
      Variable smtpPwd = tabular.getVariable("SMTP_PWD");
      smtpConnection.setPassword(smtpPwd);
    } catch (NoVariableException e) {
      // ok
    }

    String smtpDebug = tabular.getVariableAsStringOrDefault("SMTP_DEBUG", null);
    if (smtpDebug != null) {
      emailUri.addQueryProperty("debug", smtpDebug);
    }
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
