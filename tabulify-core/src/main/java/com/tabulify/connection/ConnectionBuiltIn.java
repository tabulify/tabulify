package com.tabulify.connection;

import com.tabulify.Tabular;
import com.tabulify.TabularAttribute;
import com.tabulify.memory.MemoryConnectionProvider;
import com.tabulify.noop.NoopConnectionProvider;
import net.bytle.exception.CastException;
import net.bytle.exception.IllegalArgumentExceptions;
import net.bytle.exception.IllegalStructure;
import net.bytle.fs.Fs;
import net.bytle.os.Oss;
import net.bytle.type.Casts;
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
      .addVariable("scale", 0.01);
    tabular.addConnection(tpcDs);

    // NoOp
    Connection noOp = Connection.createConnectionFromProviderOrDefault(tabular, ConnectionBuiltIn.NO_OP_CONNECTION, NoopConnectionProvider.NOOP_SCHEME)
      .setOrigin(ConnectionOrigin.BUILT_IN);
    tabular.addConnection(noOp);

    // Email
    UriEnhanced emailUri = UriEnhanced.create()
      .setScheme("smtp");

    String smtpHost = tabular.getVariable(TabularAttribute.SMTP_HOST).getValueOrDefaultAsStringNotNull();
    try {
      emailUri.setHost(smtpHost);
    } catch (IllegalStructure e) {
      throw IllegalArgumentExceptions.createFromMessage("The variable (" + TabularAttribute.SMTP_HOST + ") has a invalid value (" + smtpHost + "). Error: " + e.getMessage(), e);
    }
    String smtpPort = tabular.getVariable(TabularAttribute.SMTP_PORT).getValueOrDefaultAsStringNotNull();
    try {
      emailUri.setPort(Integers.createFromObject(smtpPort).toInteger());
    } catch (CastException e) {
      throw IllegalArgumentExceptions.createFromMessage("The variable (" + TabularAttribute.SMTP_PORT + ") has a invalid value (" + smtpPort + "). Error: " + e.getMessage(), e);
    }

    String smtpFrom = (String) tabular.getVariable(TabularAttribute.SMTP_FROM).getValueOrDefaultOrNull();
    if (smtpFrom != null) {
      emailUri.addQueryProperty("from", smtpFrom);
    } else {
      try {
        emailUri.addQueryProperty("from", Oss.getUser() + "@" + Oss.getFqdn().toStringWithoutRoot());
      } catch (UnknownHostException e) {
        // oeps
      }
    }
    String smtpFromName = (String) tabular.getVariable(TabularAttribute.SMTP_FROM_NAME).getValueOrDefaultOrNull();
    if (smtpFromName != null) {
      emailUri.addQueryProperty("from-name", smtpFrom);
    }
    String smtpTo = (String) tabular.getVariable(TabularAttribute.SMTP_TO).getValueOrDefaultOrNull();
    if (smtpTo != null) {
      emailUri.addQueryProperty("to", smtpTo);
    }
    String smtpToNames = (String) tabular.getVariable(TabularAttribute.SMTP_TO_NAMES).getValueOrDefaultOrNull();
    if (smtpToNames != null) {
      emailUri.addQueryProperty("to-names", smtpTo);
    }
    Boolean smtpAuth = (Boolean) tabular.getVariable(TabularAttribute.SMTP_AUTH).getValueOrDefaultOrNull();
    if (smtpAuth != null) {
      emailUri.addQueryProperty("auth", Casts.castSafe(smtpAuth, String.class));
    }
    Boolean smtpTls = (Boolean) tabular.getVariable(TabularAttribute.SMTP_TLS).getValueOrDefaultOrNull();
    if (smtpTls != null) {
      emailUri.addQueryProperty("tls", Casts.castSafe(smtpTls, String.class));
    }

    Connection smtpConnection = Connection.createConnectionFromProviderOrDefault(tabular, ConnectionBuiltIn.SMTP_CONNECTION, emailUri.toUri().toString())
      .setOrigin(ConnectionOrigin.BUILT_IN);
    String smtpUser = (String) tabular.getVariable(TabularAttribute.SMTP_USER).getValueOrDefaultOrNull();
    if (smtpUser != null) {
      smtpConnection.setUser(smtpUser);
    }

    Variable smtpPwd = tabular.getVariable(TabularAttribute.SMTP_USER);
    smtpConnection.addVariable(smtpPwd);


    String smtpDebug = (String) tabular.getVariable(TabularAttribute.SMTP_USER).getValueOrDefaultOrNull();
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
