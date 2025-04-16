package com.tabulify;

import com.tabulify.connection.*;
import com.tabulify.fs.FsConnection;
import com.tabulify.fs.FsDataPath;
import com.tabulify.memory.MemoryConnection;
import com.tabulify.memory.MemoryDataPath;
import com.tabulify.spi.DataPath;
import com.tabulify.tpc.TpcConnection;
import com.tabulify.uri.DataUri;
import net.bytle.exception.*;
import net.bytle.fs.Fs;
import net.bytle.java.JavaEnvs;
import net.bytle.java.Javas;
import net.bytle.regexp.Glob;
import net.bytle.type.*;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static com.tabulify.TabularAttributes.USER_CONNECTION_VAULT;
import static com.tabulify.TabularAttributes.USER_VARIABLES_FILE;

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


  private final ProjectConfigurationFile projectConfigurationFile;
  final Path variablePathArgument;
  private final Vault vault;
  private final Map<String, Connection> howtoConnections;
  private final TabularExecEnv env;
  private TabularVariables tabularVariables;

  // The default connection added to a data URI if it does not have it.
  protected Connection defaultConnection;


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


  public Tabular(String passphrase, Path projectHomePath, Path connectionVaultPath, Path variablePath, TabularExecEnv env) {


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


    if (projectHomePath == null) {
      try {
        projectHomePath = Fs.closest(Paths.get("."), ProjectConfigurationFile.PROJECT_CONF_FILE_NAME).getParent();
      } catch (FileNotFoundException e) {
        // not a project
      }
    }

    // Env
    if (env != null) {
      this.env = env;
    } else {
      String envOsValue = System.getenv(TabularOsEnv.TABLI_ENV);
      if (envOsValue != null) {
        try {
          this.env = Casts.cast(envOsValue, TabularExecEnv.class);
        } catch (CastException e) {
          throw new IllegalArgumentException("The os env (" + TabularOsEnv.TABLI_ENV + ") has a env value (" + envOsValue + ") that is unknown. Possible values: " + Enums.toConstantAsStringCommaSeparated(TabularOsEnv.class), e);
        }
      } else {
        this.env = TabularExecEnv.DEV;
      }
    }

    if (projectHomePath != null) {

      DbLoggers.LOGGER_TABULAR_START.info("This is a project run (" + projectHomePath + ")");

      Path projectFilePath = projectHomePath.resolve(ProjectConfigurationFile.PROJECT_CONF_FILE_NAME);
      if (!Files.exists(projectFilePath)) {
        DbLoggers.LOGGER_TABULAR_START.warning("The project file (" + projectFilePath + ") did not exist.");
        this.projectConfigurationFile = null;
      } else {
        try {
          this.projectConfigurationFile = ProjectConfigurationFile.createFrom(projectFilePath);
        } catch (FileNotFoundException e) {
          // should not happen
          throw new RuntimeException("The project file path (" + projectFilePath + ") does not exits");
        }
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


    this.vault = Vault.create(this, passphrase);


    ConnectionBuiltIn.loadBuiltInConnections(this);
    loadConnections(getUserConnectionVaultPath(), ConnectionOrigin.USER);
    if (connectionVaultPath != null) {
      this.connectionVaultPath = connectionVaultPath;
      loadConnections(connectionVaultPath, ConnectionOrigin.COMMAND_LINE);
    }

    if (projectConfigurationFile != null) {
      Path projectConnectionVaultPath = projectConfigurationFile.getConnectionVaultPath();
      loadConnections(projectConnectionVaultPath, ConnectionOrigin.PROJECT);
    }


    if (projectConfigurationFile != null) {

      // Default connection is project
      this.setDefaultConnection(ConnectionBuiltIn.PROJECT_CONNECTION);

    } else {

      // Default connection is cd
      this.setDefaultConnection(ConnectionBuiltIn.CD_LOCAL_FILE_SYSTEM);

    }

    this.howtoConnections = ConnectionHowTos.createHowtoConnections(this);

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

  public static Tabular tabular(String passphrase, Path projectFilePath, Path connectionVaultPath, Path variablesPath, TabularExecEnv env) {
    return new Tabular(passphrase, projectFilePath, connectionVaultPath, variablesPath, null);
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


    try (ConnectionVault connectionVault = ConnectionVault.create(this, path, this.vault)) {
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


  public Tabular addConnection(Connection connection) {

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

      if (dataUri.isScriptSelector()) {
        DataPath scriptPath = this.getDataPath(dataUri.getScriptUri().toString());
        return connection.createScriptDataPath(scriptPath);
      }

      return connection.getCurrentDataPath();

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
    return (FsConnection) getConnection(ConnectionBuiltIn.CD_LOCAL_FILE_SYSTEM);
  }


  /**
   * @return a {@link MemoryDataPath memory} data path with an id that should be unique (UUID v4)
   * TODO: May be just an sequence id implementation such as the jvm ?
   * This kind of data path is commonly used to create:
   * * a feedback table (temporary created and printed)
   * * or to get content loaded in memory such as script in test
   * * or used in test
   */
  public MemoryDataPath getAndCreateRandomMemoryDataPath() {

    return (MemoryDataPath) getConnection(ConnectionBuiltIn.MEMORY_CONNECTION).getAndCreateRandomDataPath(null);

  }

  @SuppressWarnings("unused")
  public DataPath getAndCreateRandomDataPathWithType(MediaType mediaType) {

    return getConnection(ConnectionBuiltIn.MEMORY_CONNECTION).getAndCreateRandomDataPath(mediaType);

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
    return (MemoryConnection) getConnection(ConnectionBuiltIn.MEMORY_CONNECTION);
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
   * A utility to get a local file resource data store
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


  public Boolean isIdeDev() {

    return this.env.equals(TabularExecEnv.IDE);

  }

  private Path getHomePathDynamic() {
    // in dev (with maven, the class are in the m2 repository)
    String tabliHome = System.getenv(TabularOsEnv.TABLI_HOME);
    if (tabliHome != null) {
      return Paths.get(tabliHome);
    }
    if (this.isIdeDev()) {
      try {
        // in dev (with Idea, the class are in the build directory)
        return Javas.getBuildDirectory(ConnectionHowTos.class)
          .getParent()
          .getParent();
      } catch (NotDirectoryException e) {
        throw new RuntimeException("Build directory not found. " + e.getMessage());
      }
    }
    // in prod, the class are in the jars directory
    return Javas.getSourceCodePath(ConnectionHowTos.class)
      .getParent();

  }

  /**
   * @return the home directory of the installation
   */
  public Path getHomePath() {

    if (this.homePath == null) {
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
    return (TpcConnection) getConnection(ConnectionBuiltIn.TPCDS_CONNECTION);
  }


  public void dropConnection(Connection connection) {
    Connection conn = this.connections.remove(connection.getName());
    conn.close();
  }


  public Connection getTempConnection() {
    return getConnection(ConnectionBuiltIn.TEMP_LOCAL_FILE_SYSTEM);
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


  public Path getConnectionVaultPath() {
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

      if (dataPathsByPattern.isEmpty()) {

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

    return connections.get(ConnectionBuiltIn.NO_OP_CONNECTION);
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
    return this.getConnection(ConnectionBuiltIn.LOG_LOCAL_CONNECTION);
  }

  /**
   * The execution environment
   */
  public TabularExecEnv getExecutionEnvironment() {

    return this.env;
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
    return this.getVault().createVariable(attribute, value, Origin.INTERNAL);
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


  public Connection getHowtoConnection(String name) {

    return this.howtoConnections.get(name);

  }

  public Map<String, Connection> getHowtoConnections() {
    return this.howtoConnections;
  }

  /**
   * Load the howto connection in this tabular
   * (used in test mostly)
   */
  public Tabular loadHowtoConnections() {
    this.connections.putAll(this.howtoConnections);
    return this;
  }
}
