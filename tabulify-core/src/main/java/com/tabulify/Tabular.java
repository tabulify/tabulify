package com.tabulify;

import com.tabulify.conf.*;
import com.tabulify.connection.Connection;
import com.tabulify.connection.ConnectionBuiltIn;
import com.tabulify.connection.ConnectionHowTos;
import com.tabulify.connection.ConnectionOrigin;
import com.tabulify.fs.FsConnection;
import com.tabulify.fs.FsDataPath;
import com.tabulify.memory.MemoryConnection;
import com.tabulify.memory.MemoryDataPath;
import com.tabulify.spi.DataPath;
import com.tabulify.tpc.TpcConnection;
import com.tabulify.uri.DataUri;
import net.bytle.crypto.Protector;
import net.bytle.exception.*;
import net.bytle.fs.Fs;
import net.bytle.log.Logs;
import net.bytle.regexp.Glob;
import net.bytle.type.KeyNormalizer;
import net.bytle.type.MapKeyIndependent;
import net.bytle.type.MediaType;
import net.bytle.type.Strings;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;


/**
 * A tabular is a global domain that represents the runtime environment
 * <p>
 * It's the entry point of every tabular/data application
 * It has knowledge of the {@link ConfVault}
 * and therefore is the main entry to create a data path from an URI
 * * a datastore object
 * * a connection
 * * or
 */
public class Tabular implements AutoCloseable {


  public static final String TABLI_NAME = "tabli";
  public static final String TABLI_CONF_FILE_NAME = "." + TABLI_NAME + ".yml";
  public static final Path TABLI_USER_HOME_PATH = Fs.getUserHome().resolve("." + TABLI_NAME);
  public static final Path TABLI_USER_CONF_PATH = TABLI_USER_HOME_PATH.resolve(TABLI_CONF_FILE_NAME);


  private final Vault vault;
  private final Map<String, Connection> howtoConnections;
  private final TabularExecEnv executionEnv;
  private final Path projectHomePath;
  private final TabularEnvs tabularEnvs;

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
   * The exit status
   * We may want to show what we have an error but not throw it
   * Example: of all transfers, if only one has failed
   * we will gather the transfers data to create an output
   * and then fail
   */
  private int exitStatus = 0;
  private Path runningPipelineScript;
  private final Path homePath;


  /**
   * We may not add derived generated attributes.
   * so the key identifier is not a string
   */
  private final Map<TabularAttributeEnum, Attribute> attributes = new HashMap<>();


  private final Path confPath;


  public Tabular(TabularConfig tabularConfig) {


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

    /**
     * Protector
     */
    String passphrase = TabularInit.determinePassphrase(tabularConfig.passphrase);
    Protector protector;
    if (passphrase != null) {
      protector = Protector.create(passphrase);
    } else {
      protector = null;
    }

    /**
     * All determine functions utility
     * We don't pass the tabular object so that
     * we have dependency in the function signature
     */
    tabularEnvs = new TabularEnvs(tabularConfig.templatingEnv, protector);

    /**
     * Vault
     */
    this.vault = Vault.create(protector, tabularEnvs);

    /**
     * Logs
     */
    TabularLogLevel logLevel = TabularInit.determineLogLevel(tabularConfig.logLevel, vault, tabularEnvs, attributes);
    Logs.setLevel(logLevel.getLevel());


    /**
     * Project
     */
    this.projectHomePath = TabularInit.determineProjectHome(tabularConfig.projectHome, vault, attributes, tabularEnvs);
    if (projectHomePath != null) {
      DbLoggers.LOGGER_TABULAR_START.info("This is a project run (" + projectHomePath + ")");
    } else {
      DbLoggers.LOGGER_TABULAR_START.info("This is not a project run.");
    }

    /**
     * Tabli Yaml File
     * confPath is a field because it's used by the cli
     * to modify a conf file with the cli
     */
    confPath = TabularInit.determineConfPath(tabularConfig.confPath, vault, tabularEnvs, projectHomePath);
    ConfVault confVault = ConfVault.createFromPath(confPath, vault, this);
    confVault.getConnections().forEach(this::addConnection);

    /**
     * Execution Env
     */
    this.executionEnv = TabularInit.determineEnv(tabularConfig.execEnv, vault, tabularEnvs, attributes, confVault);

    /**
     * Home Path
     */
    this.homePath = TabularInit.determineHomePath(tabularConfig.homePath, this.executionEnv, tabularEnvs, attributes, vault, confVault);

    /**
     * Other Tabular attributes, not processed
     */
    for (TabularAttributeEnum attribute : TabularAttributeEnum.class.getEnumConstants()) {

      if (attributes.containsKey(attribute)) {
        continue;
      }
      Vault.VariableBuilder variableBuilder = vault.createVariableBuilderFromAttribute(attribute);

      // Name
      // We don't look up without the tabli prefix because it can cause clashes
      // for instance, name in os is the name of the computer
      KeyNormalizer envName = tabularEnvs.getNormalizedKey(attribute);
      // Sys
      String sysEnvValue = tabularEnvs.getJavaSysValue(envName);
      if (sysEnvValue != null) {
        attributes.put(attribute,
          variableBuilder
            .setOrigin(Origin.SYS)
            .buildSafe(sysEnvValue)
        );
        continue;
      }
      // Env
      String envValue = tabularEnvs.getOsEnvValue(envName);
      if (envValue != null) {
        attributes.put(attribute,
          variableBuilder
            .setOrigin(Origin.OS)
            .buildSafe(envValue)
        );
        continue;
      }

      // no env
      attributes.put(attribute,
        variableBuilder
          .setOrigin(Origin.RUNTIME)
          .buildSafe(null)
      );

    }


    /**
     * Check for env
     */
    TabularInit.checkForEnvNotProcessed(tabularEnvs, attributes);


    /**
     * After init
     */
    Path sqliteConnectionHome = TabularInit.determineSqliteHome(vault, tabularEnvs, attributes);
    ConnectionBuiltIn.loadBuiltInConnections(this, sqliteConnectionHome);

    // Default Connection
    if (projectHomePath != null) {
      addConnection(
        Connection.createConnectionFromProviderOrDefault(this, ConnectionBuiltIn.PROJECT_CONNECTION, projectHomePath.toUri().toString())
          .setDescription("The project home path")
      );
      // Default connection is project
      this.setDefaultConnection(ConnectionBuiltIn.PROJECT_CONNECTION);
    } else {
      // Default connection is cd
      this.setDefaultConnection(ConnectionBuiltIn.CD_LOCAL_FILE_SYSTEM);
    }
    // How to connection utility
    this.howtoConnections = ConnectionHowTos.createHowtoConnections(this, sqliteConnectionHome);


  }


  public static TabularConfig tabularConfig() {
    return new TabularConfig();
  }

  public static Tabular tabular() {
    return new TabularConfig().build();
  }


  /**
   * Utility function that will return a clean environment.
   * ie delete the configurations file (ie
   * User connection and configuration)
   * <p>
   * This is mostly used in test
   */
  public static Tabular tabularWithCleanEnvironment() {

    return cleanTabularConfig().build();
  }

  public static TabularConfig cleanTabularConfig() {
    Fs.deleteIfExists(TABLI_USER_CONF_PATH);
    return Tabular.tabularConfig();
  }


  public Tabular addConnection(Connection connection) {

    connections.put(connection.getName(), connection);
    return this;


  }


  public void setDefaultConnection(Connection connection) {
    this.defaultConnection = connection;
  }

  public void setDefaultConnection(String connectionName) {
    Connection connection = getConnection(connectionName);
    if (connection != null) {
      this.defaultConnection = connection;
    } else {
      throw new RuntimeException(
        Strings.createMultiLineFromStrings("The connection (" + connectionName + ") was not found and could not be set as the default one.",
          "The actual connections are (" + getConnections().stream().map(Connection::toString).collect(Collectors.joining(", ")) + ")").toString());
    }
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
   * Utility function
   *
   * @return the memory datastore
   */
  public MemoryConnection getMemoryDataStore() {
    return (MemoryConnection) getConnection(ConnectionBuiltIn.MEMORY_CONNECTION);
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
   * @param name  - the name of the created connection (maybe null)
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
       * or uri.getScheme().equals("jar")
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


  public Boolean isIdeEnv() {

    return this.executionEnv.equals(TabularExecEnv.IDE);

  }


  /**
   * @return the home directory of the installation
   */
  public Path getHomePath() {

    return this.homePath;

  }


  /**
   * @param msg - terminate if the run is strict or print a warning message
   */
  public void warningOrTerminateIfStrict(String msg) {
    if (this.isStrict()) {
      DbLoggers.LOGGER_DB_ENGINE.warning("The run is strict, we terminate");
      throw new IllegalStateException(msg);
    }

    DbLoggers.LOGGER_DB_ENGINE.warning(msg);

  }

  /**
   * @param e Exception - terminate if the run is strict or print a warning message
   */
  public void warningOrTerminateIfStrict(Exception e) {
    if (this.isStrict()) {
      DbLoggers.LOGGER_DB_ENGINE.warning("The run is strict, we terminate");
      throw new IllegalStateException(e);
    }
    DbLoggers.LOGGER_DB_ENGINE.warning(e.getMessage());

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


  public <T> T getAttribute(TabularAttributeEnum attribute, Class<T> clazz) throws NoValueException, CastException, NoVariableException {
    Attribute variable = this.attributes.get(attribute);
    if (variable == null) {
      throw new NoValueException("The variable (" + attribute + ") was not found");
    }
    return variable.getValueOrDefaultCastAs(clazz);
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
              dataPath.addAttribute(key, value);
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


  @SuppressWarnings("unused")
  public FsDataPath getTempDirectory(String prefix) {
    return (FsDataPath) this.getTempConnection().getDataPath("tabli" + prefix + UUID.randomUUID());
  }


  public boolean isProjectRun() {
    return this.projectHomePath != null;
  }

  public Connection getLogsConnection() {
    return this.getConnection(ConnectionBuiltIn.LOG_LOCAL_CONNECTION);
  }

  /**
   * The execution environment
   */
  public TabularExecEnv getExecutionEnvironment() {

    return this.executionEnv;
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

  @SuppressWarnings("unused")
  public void removeConnection(String connectionName) {
    Connection conn = this.connections.remove(connectionName);
    conn.close();
  }

  public String getName() {
    return TABLI_NAME;
  }


  public Attribute getAttribute(TabularAttributeEnum attribute) {
    return this.attributes.get(attribute);
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


  /**
   * @return a key/attribute name in a public format
   */
  public String toPublicName(String attribute) {
    return KeyNormalizer.createSafe(attribute).toCliLongOptionName();
  }

  public String toPublicName(Attribute attribute) {
    return toPublicName(attribute.getAttributeMetadata().toString());
  }


  public Set<Attribute> getAttributes() {
    return new HashSet<>(this.attributes.values());
  }

  public Path getUserConfFilePath() {
    return TABLI_USER_HOME_PATH.resolve(TABLI_CONF_FILE_NAME);
  }

  public Path getConfPath() {
    return this.confPath;
  }

  public Attribute createAttribute(AttributeEnum attribute, Object value) {
    return this.getVault().createAttribute(attribute, value, Origin.RUNTIME);
  }

  public Attribute createAttribute(String key, Object value) throws Exception {
    return this.getVault().createAttribute(key, value, Origin.RUNTIME);
  }

  public TabularEnvs getTabularEnvs() {
    return this.tabularEnvs;
  }

  public String toPublicListOfParameters(Class<? extends AttributeEnumParameter> attributeEnumClasses) {
    return toPublicListOfParameters(Collections.singletonList(attributeEnumClasses));
  }

  public String toPublicListOfParameters(List<Class<? extends AttributeEnumParameter>> attributeEnumClasses) {
    List<AttributeEnumParameter> attributes = new ArrayList<>();
    for (Class<? extends AttributeEnumParameter> attributeEnumClass : attributeEnumClasses) {
      if (!attributeEnumClass.isEnum()) {
        throw new InternalException("An enum constant should be passed. " + attributeEnumClass.getSimpleName() + " is not an enum");
      }
      AttributeEnumParameter[] enumConstants = attributeEnumClass.getEnumConstants();
      if (enumConstants == null) {
        continue;
      }
      attributes.addAll(Arrays.asList(enumConstants));
    }
    return attributes
      .stream()
      .filter(AttributeEnumParameter::isParameter)
      .map(enumValue -> toPublicName(enumValue.toString()))
      .collect(Collectors.joining(", "));
  }


  public static class TabularConfig {
    private Path homePath;
    private String passphrase;
    private Path projectHome;
    private Path confPath;
    private TabularExecEnv execEnv;
    private final Map<String, String> templatingEnv = new HashMap<>();
    private TabularLogLevel logLevel;

    public TabularConfig setPassphrase(String passphrase) {
      this.passphrase = passphrase;
      return this;
    }

    public TabularConfig setProjectHome(Path projectHome) {
      this.projectHome = projectHome;
      return this;
    }

    public TabularConfig setConf(Path confPath) {
      this.confPath = confPath;
      return this;
    }

    public TabularConfig setExecEnv(TabularExecEnv execEnv) {
      this.execEnv = execEnv;
      return this;
    }

    @SuppressWarnings("unused")
    public TabularConfig setHomePath(Path homePath) {
      this.homePath = homePath;
      return this;
    }

    public Tabular build() {
      return new Tabular(this);
    }

    public TabularConfig setLogLevel(TabularLogLevel logLevel) {
      this.logLevel = logLevel;
      return this;
    }
  }
}
