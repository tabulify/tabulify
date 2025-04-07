package net.bytle.db;

import net.bytle.db.connection.Connection;
import net.bytle.db.connection.ConnectionHowTos;
import net.bytle.db.connection.ConnectionOrigin;
import net.bytle.db.connection.ConnectionVault;
import net.bytle.db.fs.FsConnection;
import net.bytle.db.fs.FsDataPath;
import net.bytle.db.memory.MemoryConnection;
import net.bytle.db.memory.MemoryConnectionProvider;
import net.bytle.db.memory.MemoryDataPath;
import net.bytle.db.noop.NoopConnectionProvider;
import net.bytle.db.spi.DataPath;
import net.bytle.db.tpc.TpcConnection;
import net.bytle.db.uri.DataUri;
import net.bytle.exception.*;
import net.bytle.fs.Fs;
import net.bytle.java.JavaEnvs;
import net.bytle.java.Javas;
import net.bytle.os.Oss;
import net.bytle.regexp.Glob;
import net.bytle.type.*;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.file.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static net.bytle.db.TabularAttributes.USER_CONNECTION_VAULT;
import static net.bytle.db.TabularAttributes.USER_VARIABLES_FILE;

/**
 * A tabular is a global domain that represents the runtime environment
 * <p>
 * It's the entry point of every tabular/data application
 * It has knowledge of the {@link ConnectionVault}
 * and therefore is the main entry to create a data path from an URI
 * * a datastore object
 * * a connection
 * * or
 */
public class Tabular implements AutoCloseable {


  public static final String PROJECT_CONF_FILE_NAME = ".tabli.yml";
  private final ProjectConfigurationFile projectConfigurationFile;
  final Path variablePathArgument;
  private final Vault vault;
  private TabularVariables tabularVariables;

  // The default connection added to a data URI if it does not have it.
  protected Connection defaultConnection;

  // The internal connection
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
   * Connections
   */
  final MapKeyIndependent<Connection> connections = new MapKeyIndependent<>();


  /**
   * If the run is strict, the run will not try to correct itself.
   * <p>
   * If the run is not strict, the run will try to correct itself.
   * For instance, if a file exists already, it will not overwrite it.
   */
  private Boolean strict = true;

  /**
   * The path of the data store vault
   */
  private Path connectionVaultPath;

  /**
   * The exit status
   * We may want to show what we have and not throw an error
   * Example: of all transfers, if only one has failed
   * we will gather the transfers data to create an output
   * and then fail
   */
  private int exitStatus = 0;
  private Path runningPipelineScript;
  private Path homePath;


  public Tabular(String passphrase, Path projectFilePath, Path connectionVaultPath, Path variablePath, String env) {


    /**
     * To avoid
     * Dec 10, 2020 1:23:32 PM oracle.jdbc.driver.OracleDriver registerMBeans
     * WARNING: Error while registering Oracle JDBC Diagnosability MBean.
     * java.security.AccessControlException: access denied ("javax.management.MBeanTrustPermission" "register")
     * 	at java.security.AccessControlContext.checkPermission(AccessControlContext.java:472)
     * 	at java.lang.SecurityManager.checkPermission(SecurityManager.java:585)
     * 	at com.sun.jmx.interceptor.DefaultMBeanServerInterceptor.checkMBeanTrustPermission(DefaultMBeanServerInterceptor.java:1848)
     * 	at com.sun.jmx.interceptor.DefaultMBeanServerInterceptor.registerMBean(DefaultMBeanServerInterceptor.java:322)
     * 	at com.sun.jmx.mbeanserver.JmxMBeanServer.registerMBean(JmxMBeanServer.java:522)
     */
    Logger.getLogger("oracle.jdbc").setLevel(Level.SEVERE);


    if (projectFilePath == null) {
      try {
        projectFilePath = Fs.closest(Paths.get("."), PROJECT_CONF_FILE_NAME);
      } catch (FileNotFoundException e) {
        // not a project
      }
    }


    if (projectFilePath != null) {

      DbLoggers.LOGGER_TABULAR_START.info("This is a project run (" + projectFilePath + ")");

      if (!Files.exists(projectFilePath)) {
        throw new RuntimeException("The project file (" + projectFilePath + ") did not exist.");
      }

      try {
        this.projectConfigurationFile = ProjectConfigurationFile.createFrom(projectFilePath, env);
      } catch (FileNotFoundException e) {
        // should not happen
        throw new RuntimeException("The project file path (" + projectFilePath + ") does not exits");
      }


    } else {

      DbLoggers.LOGGER_TABULAR_START.info("This is not a project run.");
      this.projectConfigurationFile = null;

    }

    /**
     * Load variables environment first
     * (It's used by the {@link Vault)
     * Configuration file
     * lower priority first
     * ie host, project, tabli add the command line conf
     */
    this.variablePathArgument = variablePath;
    this.tabularVariables = TabularVariables.create(this, projectConfigurationFile);


    /**
     * If passphrase is null, the vault protect with a default passphrase
     */
    this.vault = Vault.create(this, passphrase);

    /**
     * Connection (should be created after the {@link Vault})
     * Loaded by order of precedence
     */
    if (projectFilePath != null) {
      String localFileUrl = projectFilePath
        .getParent()
        .toAbsolutePath()
        .normalize()
        .toUri()
        .toString();
      this.createRuntimeConnection(Tabular.PROJECT_CONNECTION, localFileUrl)
        .setDescription("The project home directory");
    }

    loadBuiltInConnections();
    loadConnections(getUserConnectionVaultPath(), ConnectionOrigin.USER);
    if (connectionVaultPath != null) {
      this.connectionVaultPath = connectionVaultPath;
      loadConnections(connectionVaultPath, ConnectionOrigin.COMMAND_LINE);
    }

    if (projectConfigurationFile != null) {
      Path projectConnectionVaultPath = projectConfigurationFile.getConnectionVaultPath();
      loadConnections(projectConnectionVaultPath, ConnectionOrigin.PROJECT);
    }


    if (projectFilePath != null) {

      // Default connection is project
      this.setDefaultConnection(Tabular.PROJECT_CONNECTION);

    } else {

      // Default connection is cd
      this.setDefaultConnection(Tabular.CD_LOCAL_FILE_SYSTEM);

    }


  }


  public static Tabular tabular(String passphrase) {
    return new Tabular(passphrase, null, null, null, null);
  }

  public static Tabular tabular(String passphrase, Path projectFilePath) {
    return new Tabular(passphrase, projectFilePath, null, null, null);
  }

  public static Tabular tabular(String passphrase, Path projectFilePath, Path connectionVaultPath) {
    return new Tabular(passphrase, projectFilePath, connectionVaultPath, null, null);
  }

  /**
   * TODO: We could just make a builder
   *
   * @param passphrase          the passphrase
   * @param projectFilePath     the project file
   * @param connectionVaultPath the connection vault path
   * @param variablesPath       the variable path
   * @return the tabular object for chaining
   */
  public static Tabular tabular(String passphrase, Path projectFilePath, Path connectionVaultPath, Path variablesPath) {
    return new Tabular(passphrase, projectFilePath, connectionVaultPath, variablesPath, null);
  }

  public static Tabular tabular(String passphrase, Path projectFilePath, Path connectionVaultPath, Path variablesPath, String env) {
    return new Tabular(passphrase, projectFilePath, connectionVaultPath, variablesPath, env);
  }

  /**
   * @param passphrase the passphrase
   * @return a tabular without the user configuration files
   */
  public static Tabular tabularWithCleanEnvironment(String passphrase) {
    Tabular.cleanEnvironment();
    return tabular(passphrase);
  }

  public static void cleanEnvironment() {
    Path userConnectionVaultPath = (Path) USER_CONNECTION_VAULT.getDefaultValue();
    Fs.deleteIfExists(userConnectionVaultPath);
    Path userVariablePath = (Path) USER_VARIABLES_FILE.getDefaultValue();
    Fs.deleteIfExists(userVariablePath);
  }


  private void loadConnections(Path path, ConnectionOrigin connectionOrigin) {


    try (ConnectionVault connectionVault = new ConnectionVault(this, path)) {
      connectionVault
        .getConnections()
        .stream()
        .map(c -> c.setOrigin(connectionOrigin))
        .forEach(c -> {
          Connection actualConnection = connections.get(c.getName());
          if (actualConnection != null) {
            if (actualConnection.getOrigin() == ConnectionOrigin.BUILT_IN) {
              throw new RuntimeException("You can't redeclare the built-in connection (" + actualConnection + "). Delete it or rename it in the (" + connectionOrigin + ") connection vault.");
            } else {
              DbLoggers.LOGGER_DB_ENGINE.warning("The connection (" + actualConnection + ") declared in the vault (" + actualConnection.getOrigin() + ") has been replaced by the connection declared in the vault (" + connectionOrigin + ")");
            }
          }
          this.addConnection(c);
        });
    }

  }


  /**
   * Create the built-in, internal connections
   */
  private void loadBuiltInConnections() {
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
      .createConnectionFromProviderOrDefault(this, CD_LOCAL_FILE_SYSTEM, localFileUrl)
      .setDescription("The local file system")
      .setOrigin(ConnectionOrigin.BUILT_IN);
    this.addConnection(localConnection);


    // Local temporary Directory
    String localTempUrl = Fs.getTempDirectory()
      .toAbsolutePath()
      .normalize()
      .toUri()
      .toString();
    Connection temp = Connection.createConnectionFromProviderOrDefault(this, TEMP_LOCAL_FILE_SYSTEM, localTempUrl)
      .setDescription("The local temporary directory of the local file system")
      .setOrigin(ConnectionOrigin.BUILT_IN);
    this.addConnection(temp);
    // Local temporary Directory

    String localUserUrl = Fs.getUserHome()
      .toAbsolutePath()
      .normalize()
      .toUri()
      .toString();
    Connection user = Connection.createConnectionFromProviderOrDefault(this, HOME_LOCAL_FILE_SYSTEM, localUserUrl)
      .setDescription("The user home directory of the local file system")
      .setOrigin(ConnectionOrigin.BUILT_IN);
    this.addConnection(user);

    Path logDbPath = Fs.getUserAppData(TabularAttributes.APP_NAME.toString()).resolve(LOG_LOCAL_CONNECTION + ".db");
    String rootWindows = "///";
    String localLogsUriString = "jdbc:sqlite:" + rootWindows + logDbPath.toString().replace("\\", "/");
    Connection logs = Connection.createConnectionFromProviderOrDefault(this, LOG_LOCAL_CONNECTION, localLogsUriString)
      .setDescription("The tabli logs")
      .setOrigin(ConnectionOrigin.BUILT_IN);
    this.addConnection(logs);

    String localDesktopUrl = Fs.getUserDesktop()
      .toAbsolutePath()
      .normalize()
      .toUri()
      .toString();
    Connection desktop = Connection.createConnectionFromProviderOrDefault(this, DESKTOP_LOCAL_FILE_SYSTEM, localDesktopUrl)
      .setDescription("The user desktop directory of the local file system")
      .setOrigin(ConnectionOrigin.BUILT_IN);
    this.addConnection(desktop);

    // Memory
    Connection memoryConnection = Connection.createConnectionFromProviderOrDefault(this, MEMORY_CONNECTION, MemoryConnectionProvider.SCHEME)
      .setOrigin(ConnectionOrigin.BUILT_IN);
    this.addConnection(memoryConnection);

    this.setDefaultConnection(memoryConnection);

    // TpcsDs
    Connection tpcDs = Connection.createConnectionFromProviderOrDefault(this, TPCDS_CONNECTION, TPCDS_CONNECTION)
      .setOrigin(ConnectionOrigin.BUILT_IN)
      .addVariable("scale", 0.01);
    this.addConnection(tpcDs);

    // NoOp
    Connection noOp = Connection.createConnectionFromProviderOrDefault(this, NO_OP_CONNECTION, NoopConnectionProvider.NOOP_SCHEME)
      .setOrigin(ConnectionOrigin.BUILT_IN);
    this.addConnection(noOp);

    // Email
    UriEnhanced emailUri = UriEnhanced.create()
      .setScheme("smtp");

    String smtpHostKey = "SMTP_HOST";
    String smtpHost = this.getVariableAsStringOrDefault(smtpHostKey, "localhost");
    try {
      emailUri.setHost(smtpHost);
    } catch (IllegalStructure e) {
      throw IllegalArgumentExceptions.createFromMessage("The environment variable (" + smtpHostKey + ") has a invalid value (" + smtpHost + "). Error: " + e.getMessage(), e);
    }
    String smtpPortEnv = "SMTP_PORT";
    String smtpPort = this.getVariableAsStringOrDefault(smtpPortEnv, "25");
    try {
      emailUri.setPort(Integers.createFromObject(smtpPort).toInteger());
    } catch (CastException e) {
      throw IllegalArgumentExceptions.createFromMessage("The environment variable (" + smtpPortEnv + ") has a invalid value (" + smtpPort + "). Error: " + e.getMessage(), e);
    }

    String smtpFrom = this.getVariableAsStringOrDefault("SMTP_FROM", null);
    if (smtpFrom != null) {
      emailUri.addQueryProperty("from", smtpFrom);
    } else {
      try {
        emailUri.addQueryProperty("from", Oss.getUser() + "@" + Oss.getFqdn().toStringWithoutRoot());
      } catch (UnknownHostException e) {
        // oeps
      }
    }
    String smtpFromName = this.getVariableAsStringOrDefault("SMTP_FROM_NAME", null);
    if (smtpFromName != null) {
      emailUri.addQueryProperty("from-name", smtpFrom);
    }
    String smtpTo = this.getVariableAsStringOrDefault("SMTP_TO", null);
    if (smtpTo != null) {
      emailUri.addQueryProperty("to", smtpTo);
    }
    String smtpToNames = this.getVariableAsStringOrDefault("SMTP_TO_NAMES", null);
    if (smtpToNames != null) {
      emailUri.addQueryProperty("to-names", smtpTo);
    }
    String smtpAuth = this.getVariableAsStringOrDefault("SMTP_AUTH", null);
    if (smtpAuth != null) {
      emailUri.addQueryProperty("auth", smtpAuth);
    }
    String smtpTls = this.getVariableAsStringOrDefault("SMTP_TLS", null);
    if (smtpTls != null) {
      emailUri.addQueryProperty("tls", smtpTls);
    }

    Connection smtpConnection = Connection.createConnectionFromProviderOrDefault(this, SMTP_CONNECTION, emailUri.toUri().toString())
      .setOrigin(ConnectionOrigin.BUILT_IN);
    String smtpUser = this.getVariableAsStringOrDefault("SMTP_USER", null);
    if (smtpUser != null) {
      smtpConnection.setUser(smtpUser);
    }

    try {
      Variable smtpPwd = this.getVariable("SMTP_PWD");
      smtpConnection.setPassword(smtpPwd);
    } catch (NoVariableException e) {
      // ok
    }

    String smtpDebug = this.getVariableAsStringOrDefault("SMTP_DEBUG", null);
    if (smtpDebug != null) {
      emailUri.addQueryProperty("debug", smtpDebug);
    }


    this.addConnection(smtpConnection);

  }

  private Tabular addConnection(Connection connection) {

    connections.put(connection.getName(), connection);
    return this;


  }

  /**
   * @param name - the variable name
   * @param def  - the default
   * @return the value or the default if not found
   */
  public String getVariableAsStringOrDefault(String name, String def) {

    try {
      return this.getVariable(name, String.class);
    } catch (NoValueException | CastException e) {
      return def;
    }

  }

  public void setDefaultConnection(Connection connection) {
    this.defaultConnection = connection;
  }

  public void setDefaultConnection(String dataStoreName) {
    Connection connection = getConnection(dataStoreName);
    if (connection != null) {
      this.defaultConnection = connection;
    } else {
      throw new RuntimeException(
        Strings.createMultiLineFromStrings("The data store (" + dataStoreName + ") was not found and could not be set as the default one.",
          "The actual datastore are (" + getConnections().stream().map(Connection::toString).collect(Collectors.joining(", ")) + ")").toString());
    }
  }

  public static Tabular tabular() {
    return new Tabular(null, null, null, null, null);
  }


  /**
   * @return all connections
   */
  public Set<Connection> getConnections() {

    return new HashSet<>(connections.values());

  }

  public List<Connection> selectConnections(String... globPatterns) {
    return getConnections()
      .stream()
      .filter(
        ds -> Arrays
          .stream(globPatterns)
          .anyMatch(gp -> Glob.createOf(gp).matches(ds.toString()))
      )
      .collect(Collectors.toList());
  }

  public DataPath getDataPath(String dataUri) {
    return getDataPath(dataUri, null);
  }

  public DataPath getDataPath(String uriOrDataUriString, MediaType mediaType) {

    DataUri dataUriObj = this.createDataUri(uriOrDataUriString);
    return getDataPath(dataUriObj, mediaType);

  }

  /**
   * @param dataUri   - A data uri defining the first data path
   * @param mediaType - The media type
   * @return the data path
   */
  public DataPath getDataPath(DataUri dataUri, MediaType mediaType) {

    Connection connection = dataUri.getConnection();
    String path;
    try {
      path = dataUri.getPath();
    } catch (NoPathFoundException e) {
      return connection.getCurrentDataPath();
    }

    if (dataUri.isScriptSelector()) {
      DataPath scriptPath = this.getDataPath(path);
      return connection.createScriptDataPath(scriptPath);
    }
    return connection.getDataPath(path, mediaType);


  }

  /**
   * @param spec - an URI or a data URI
   * @return the data uri object
   */
  public DataUri createDataUri(String spec) {


    return DataUri.createFromString(this, spec);

  }

  /**
   * @param connectionName - the name of the connection
   * @return a connection or null
   */
  public Connection getConnection(String connectionName) {

    Objects.requireNonNull(connectionName, "The connection name is null. Internal error, you may want to create a qualified URI before to create the connection");

    return this.connections.get(connectionName);

  }


  public Connection createRuntimeConnection(String connectionName, String uri) {
    Connection connection = Connection
      .createConnectionFromProviderOrDefault(this, connectionName, uri)
      .setOrigin(ConnectionOrigin.RUNTIME);
    this.connections.put(connectionName, connection);
    return connection;
  }


  /**
   * Close resources
   */
  public void close() {

    for (Connection connection : this.getConnections()) {
      connection.close();
    }

  }

  /**
   * A utility function to get a data path from a nio path
   *
   * @param path - the path
   * @return the file system path
   */
  public FsDataPath getDataPath(Path path) {

    DataUri dataUri = DataUri.createFromString(this, path.toUri().toString());
    try {
      return (FsDataPath) dataUri.getConnection().getDataPath(dataUri.getPath(), null);
    } catch (NoPathFoundException e) {
      throw new InternalException("The path should be available as we give it");
    }

  }


  public List<DataPath> select(DataUri dataSelector) {
    return select(dataSelector, null);
  }

  public List<DataPath> select(String dataUriSelector) {

    return select(DataUri.createFromString(this, dataUriSelector), null);
  }


  /**
   * A utility function that returns data paths selected from a data uri pattern (ie glob_pattern@datastore)
   * <p>
   * This function takes into account:
   * * the query uri pattern (ie queryPattern*.sql@datastore, `select 1@datastore`)
   * * and a entity uri pattern (ie table*@datastore)
   *
   * @param dataSelector - a data selector
   * @return the data paths that the data uri pattern selects
   */
  public List<DataPath> select(DataUri dataSelector, MediaType mediaType) {

    /**
     * The return object
     */
    List<DataPath> dataPathsToReturn = new ArrayList<>();

    Connection connection = dataSelector.getConnection();

    /**
     * non data system traversal supported
     */
    List<String> schemesWhereTraversalIsNotSupported = Arrays.asList("http", "https");
    if (schemesWhereTraversalIsNotSupported.contains(connection.getScheme())) {
      return Collections.singletonList(this.getDataPath(dataSelector, mediaType));
    }

    /**
     * Path part
     */
    // Script ? (Command or query)
    if (dataSelector.isScriptSelector()) {

      /**
       * The path part of the data uri defines a command or a query
       */
      List<DataPath> commandDataPaths = select(dataSelector.getScriptUri(), null);
      for (DataPath commandDataPath : commandDataPaths) {
        dataPathsToReturn.add(connection.createScriptDataPath(commandDataPath));
      }

    } else {

      String pattern;
      try {
        pattern = dataSelector.getPattern();
      } catch (NoPatternFoundException e) {
        throw new RuntimeException("A glob is mandatory when selecting and was not found on the selector (" + dataSelector + "). Example with the star: *@datastore");
      }
      dataPathsToReturn = connection.select(pattern, mediaType);

    }
    return dataPathsToReturn;
  }


  public Connection getDefaultConnection() {
    return this.defaultConnection;
  }

  /**
   * Utility function
   *
   * @return the local file system
   */
  public FsConnection getCurrentLocalDirectoryConnection() {
    return (FsConnection) getConnection(CD_LOCAL_FILE_SYSTEM);
  }


  /**
   * @return a {@link net.bytle.db.memory.MemoryDataPath memory} data path with an id that should be unique (UUID v4)
   * TODO: May be just an sequence id implementation such as the jvm ?
   * This kind of data path is commonly used to create:
   * * a feedback table (temporary created and printed)
   * * or to get content loaded in memory such as script in test
   * * or used in test
   */
  public MemoryDataPath getAndCreateRandomMemoryDataPath() {

    return (MemoryDataPath) getConnection(MEMORY_CONNECTION).getAndCreateRandomDataPath(null);

  }

  @SuppressWarnings("unused")
  public DataPath getAndCreateRandomDataPathWithType(MediaType mediaType) {

    return getConnection(MEMORY_CONNECTION).getAndCreateRandomDataPath(mediaType);

  }


  /**
   * Set the strict hood of the run
   *
   * @param strict - if the execution is strict or not
   */
  public void setStrict(Boolean strict) {
    this.strict = strict;
  }

  /**
   * Return if it's a strict run
   */
  public boolean isStrict() {
    return this.strict;
  }


  /**
   * Reload data from disk (ie {@link ConnectionVault}
   * This is mostly used in test
   * to test the command line client operation
   */
  public void reload() {

    /**
     * We may need to track the configuration file
     */
    this.tabularVariables = TabularVariables.create(this, projectConfigurationFile);

  }


  /**
   * Utility function
   *
   * @return the memory datastore
   */
  public MemoryConnection getMemoryDataStore() {
    return (MemoryConnection) getConnection(MEMORY_CONNECTION);
  }


  /**
   * @return the default data store path
   */
  public Path getUserConnectionVaultPath() {
    try {
      return (Path) this.getEnvVariables().getVariable(USER_CONNECTION_VAULT).getValueOrDefault();
    } catch (NoVariableException | NoValueException e) {
      // should not happen
      throw new InternalException(e);
    }
  }


  /**
   * Utility function that will return a clean environment.
   * ie delete the configurations file (ie
   * User connection and configuration)
   * <p>
   * This is mostly used in test
   */
  public static Tabular tabularWithCleanEnvironment() {

    return tabularWithCleanEnvironment(null);
  }

  public Tabular setExitStatus(int exitStatus) {
    this.exitStatus = exitStatus;
    return this;
  }

  public int getExitStatus() {
    return exitStatus;
  }


  /**
   * An utility to get a local file resource data store
   *
   * @param clazz - the class to locate the resource for
   * @param root  - the root directory (mandatory otherwise the root is not on the resources directory)
   * @return the connection
   */
  public FsConnection createRuntimeConnectionForResources(Class<?> clazz, String root) {
    return createRuntimeConnectionForResourcesWithName(clazz, root, null);
  }

  /**
   * @param clazz - the class
   * @param root  - the resource root
   * @param name  - the name of the created connection (may be null)
   * @return the connection created
   */
  public FsConnection createRuntimeConnectionForResourcesWithName(Class<?> clazz, String root, String name) {
    try {
      if (!(root.startsWith("/"))) {
        root = "/" + root;
      }
      URL resource = clazz.getResource(root);
      if (resource == null) {
        throw new IllegalArgumentException("The root directory resource (" + root + ") was not found");
      }

      URI uri = resource.toURI();

      /**
       * Load the file system
       * (For zip, this is not done automatically)
       * <p>
       * To avoid
       * java.nio.file.FileSystemAlreadyExistsException
       * at com.sun.nio.zipfs.ZipFileSystemProvider.newFileSystem(ZipFileSystemProvider.java:113)
       *
       */
      if (!uri.getScheme().equals("file")) {
        try {
          FileSystems.getFileSystem(uri);
        } catch (FileSystemNotFoundException e) {
          try {
            // as we retrieve it later
            //noinspection resource
            FileSystems.newFileSystem(uri, new HashMap<>());
          } catch (IOException ex) {
            throw new RuntimeException("Unable to instantiate the file system for the uri (" + uri + ")", ex);
          }
        }
      }

      if (name == null) {
        name = Connection.getConnectionNameFromUri(uri);
      }
      Connection connection = this.getConnection(name);
      if (connection == null) {
        String url = uri.toString();
        connection = createRuntimeConnection(name, url);
      }
      return (FsConnection) connection;
    } catch (URISyntaxException e) {
      throw new RuntimeException("Uri error " + e.getMessage(), e);
    }
  }


  public Boolean isDev() {
    return JavaEnvs.isDev(Tabular.class);
  }

  private Path getHomePathDynamic(){
    try {
      // in dev
      return Javas.getBuildDirectory(ConnectionHowTos.class)
        .getParent()
        .getParent();
    } catch (NotDirectoryException e) {
      // in prod
      return Javas.getSourceCodePath(JavaEnvs.class)
        .getParent()
        .getParent();
    }
  }
  /**
   * @return the home directory of the installation
   */
  public Path getHomePath() {

    if(this.homePath ==null){
      this.homePath = this.getHomePathDynamic();
    }
    return this.homePath;

  }


  /**
   * @param msg - terminate if the run is strict or print a warning message
   */
  public void warningOrTerminateIfStrict(String msg) {
    if (!this.isStrict()) {
      DbLoggers.LOGGER_DB_ENGINE.warning(msg);
    } else {
      DbLoggers.LOGGER_DB_ENGINE.warning("The run is strict, we terminate");
      throw new IllegalStateException(msg);
    }
  }


  public TpcConnection getTpcConnection() {
    return (TpcConnection) getConnection(TPCDS_CONNECTION);
  }


  public void dropConnection(Connection connection) {
    Connection conn = this.connections.remove(connection.getName());
    conn.close();
  }


  public Connection getTempConnection() {
    return getConnection(TEMP_LOCAL_FILE_SYSTEM);
  }


  public <T> T getVariable(String key, Class<T> clazz) throws NoValueException, CastException {
    Variable variable = this.tabularVariables.getVariable(key);
    if (variable == null) {
      throw new NoValueException("The variable (" + key + ") was not found");
    }
    return variable.getValueOrDefaultCastAs(clazz);
  }

  public <T> T getVariable(Attribute attribute, Class<T> clazz) throws NoValueException, CastException, NoVariableException {
    Variable variable = this.tabularVariables.getVariable(attribute);
    if (variable == null) {
      throw new NoValueException("The variable (" + attribute + ") was not found");
    }
    return variable.getValueOrDefaultCastAs(clazz);
  }


  public Path getConnectionVault() {
    if (this.connectionVaultPath != null) {
      return this.connectionVaultPath;
    } else {
      return getUserConnectionVaultPath();
    }
  }

  public Set<DataPath> select(Set<DataUri> dataSelectors, boolean isStrict, MediaType mediaType) {

    Set<DataPath> dataPathSet = new HashSet<>();

    for (DataUri dataUriSelector : dataSelectors) {

      List<DataPath> dataPathsByPattern = this.select(dataUriSelector, mediaType);

      if (dataPathsByPattern.size() == 0) {

        /**
         * This is a fine level because a data selector may define a target uri.
         * <p>
         * For instance, in the fill operation,
         * the target data selector may be just a target uri
         * and this is a little confusing if we show up to the user
         * as a warning or info
         *
         */
        String msg = "The data uri selector (" + dataUriSelector + ") does not select data resources";
        DbLoggers.LOGGER_DB_ENGINE.fine(msg);
        if (isStrict) {
          throw new RuntimeException(msg);
        }

      } else {

        /**
         * Glob
         * Adding the glob backreference to the data path attributes
         * ie 1, 2, 3
         */
        boolean containsWildCard = false;
        String pattern = null;
        try {
          pattern = dataUriSelector.getPattern();
          containsWildCard = Glob.createOf(pattern).containsGlobWildCard();
        } catch (NoPatternFoundException e) {
          // no pattern
        }

        if (containsWildCard) {

          if (dataUriSelector.isScriptSelector()) {
            try {
              pattern = dataUriSelector.getScriptUri().getPattern();
            } catch (NoPatternFoundException e) {
              throw new IllegalArgumentException("A pattern or path is mandatory with a script data uri");
            }
            /**
             * Because the pattern of a composed data selector
             * has a lot of chance to be a file system, if the source data store
             * is a database, we just take the first name
             * <p>
             * To avoid:
             * The stringPath (sqlite/*.sql) is not a relative one
             * and has 2 names but the datastore (sqlite) supports only 1.
             */
            int beginIndex = pattern.lastIndexOf("/");
            if (beginIndex != -1) {
              pattern = pattern.substring(beginIndex + 1);
            }
          }

          for (DataPath dataPath : dataPathsByPattern) {
            Glob glob = dataPath.getConnection()
              .createStringPath(pattern)
              .standardize()
              .toGlobExpression();
            /**
             * The match is against the relative connection path
             */
            List<String> groups = glob.getGroups(dataPath.getRelativePath());
            for (int i = 1; i < groups.size(); i++) {
              String key = String.valueOf(i);
              String value = groups.get(i);
              dataPath.addVariable(key, value);
            }
          }
        }
        dataPathSet.addAll(dataPathsByPattern);

      }
    }
    return dataPathSet;
  }


  public Connection getNoOpConnection() {

    return connections.get(NO_OP_CONNECTION);
  }


  public FsConnection createRuntimeConnectionFromLocalPath(String connectionName, Path localPath) {
    String uri = localPath.toAbsolutePath()
      .normalize()
      .toUri()
      .toString();
    return (FsConnection) this.createRuntimeConnection(connectionName, uri);
  }


  public ProjectConfigurationFile getProjectConfigurationFile() {
    return this.projectConfigurationFile;
  }

  @SuppressWarnings("unused")
  public FsDataPath getTempDirectory(String prefix) {
    return (FsDataPath) this.getTempConnection().getDataPath("tabli" + prefix + UUID.randomUUID());
  }

  public TabularVariables getEnvVariables() {
    return this.tabularVariables;
  }

  public boolean isProjectRun() {
    return this.projectConfigurationFile != null;
  }

  public Connection getLogsConnection() {
    return this.getConnection(LOG_LOCAL_CONNECTION);
  }

  public String getEnvironment() {
    ProjectConfigurationFile projectConfigurationFile = this.projectConfigurationFile;
    if (projectConfigurationFile == null) {
      return ProjectConfigurationFile.NONE_ENV;
    }
    return projectConfigurationFile.getEnvironment();
  }


  public DataPath getTempFile(String prefix, String suffix) {
    return this.getTempConnection().getDataPath(prefix + UUID.randomUUID() + suffix);
  }

  public DataUri getDefaultUri() {
    return DataUri.createFromConnectionAndPath(this.getDefaultConnection(), "");
  }

  /**
   * Used to indicate the running script
   * to be able to use the {@link Connection}
   *
   * @param path - the path of the actual running or parsed pipeline script
   * @return the object for chaining
   */
  public Tabular setParsedPipelineScript(Path path) {
    this.runningPipelineScript = path;
    return this;
  }

  public Path getRunningPipelineScript() {
    return this.runningPipelineScript;
  }

  public Connection getSdConnection() {
    Path path = this.getRunningPipelineScript();
    if (path == null) {
      throw new RuntimeException("Internal Error: The running pipeline is unknown but a data uri string uses a `sd` connection");
    }
    return this.createRuntimeConnectionFromLocalPath(Connection.getConnectionNameFromUri(path.getParent().toUri()), path.getParent());
  }

  public Vault getVault() {
    return this.vault;
  }

  public void removeConnection(String connectionName) {
    Connection conn = this.connections.remove(connectionName);
    conn.close();
  }

  public Variable getName() {
    try {
      return this.getEnvVariables().getVariable(TabularAttributes.APP_NAME);
    } catch (Exception | NoVariableException e) {
      // ok should not happen
      throw new RuntimeException("Internal Error, the name should exists", e);
    }
  }

  public Variable createVariable(String key, Object value) throws Exception {
    return this.getVault().createVariable(key, value);
  }

  public Variable createVariable(Attribute attribute, Object value) throws Exception {
    return this.getVault().createVariable(attribute, value);
  }

  public Variable getVariable(Attribute attribute) throws NoVariableException {
    return getVariable(attribute.toString());
  }

  public Variable getVariable(String attribute) throws NoVariableException {
    Variable variable = this.tabularVariables.getVariable(attribute);
    if (variable == null) {
      throw new NoVariableException();
    }
    return variable;
  }

  public Tabular setVariable(Attribute attribute, Object value) {

    try {
      this.getVariable(attribute).setOriginalValue(value);
    } catch (NoVariableException e) {
      try {
        this.createVariable(attribute, value);
      } catch (Exception ex) {
        throw new RuntimeException("Error while trying to create the variable (" + attribute + ")" + e.getMessage(), e);
      }
    }

    return this;
  }

  public MemoryDataPath createMemoryDataPath(String path) {
    return this.getMemoryDataStore().getDataPath(path);
  }

  @SuppressWarnings("UnusedReturnValue")
  public Tabular addConnection(String name, Connection connection) {
    this.connections.put(name, connection);
    return this;
  }

}
