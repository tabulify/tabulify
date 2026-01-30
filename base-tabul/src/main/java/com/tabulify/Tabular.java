package com.tabulify;

import com.tabulify.conf.*;
import com.tabulify.connection.Connection;
import com.tabulify.connection.ConnectionBuiltIn;
import com.tabulify.connection.ObjectOrigin;
import com.tabulify.fs.FsConnection;
import com.tabulify.fs.FsDataPath;
import com.tabulify.glob.Glob;
import com.tabulify.howto.Howtos;
import com.tabulify.memory.MemoryConnection;
import com.tabulify.memory.MemoryDataPath;
import com.tabulify.service.Service;
import com.tabulify.spi.DataPath;
import com.tabulify.spi.SelectionStrictException;
import com.tabulify.tpc.TpcConnection;
import com.tabulify.uri.DataUriBuilder;
import com.tabulify.uri.DataUriNode;
import com.tabulify.uri.DataUriStringNode;
import com.tabulify.crypto.Protector;
import com.tabulify.exception.*;
import com.tabulify.fs.Fs;
import com.tabulify.type.KeyNormalizer;
import com.tabulify.type.MediaType;
import com.tabulify.type.MediaTypes;
import com.tabulify.type.Strings;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.*;
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

    private static final Logger TABULIFY_PACKAGE_LOGGER = Logger.getLogger(Tabular.class.getPackage().getName());

    public static final String TABUL_NAME = "tabul";
    public static final String TABUL_CONF_FILE_NAME = "." + TABUL_NAME + ".yml";
    // Hack to have a consistent os user home in the documentation
    public static final String TABUL_OS_USER_HOME = TABUL_NAME.toUpperCase(Locale.ROOT) + "_OS_USER_HOME";


    private final Vault vault;
    private final TabularExecEnv executionEnv;
    private final Path appHomePath;
    private final TabularEnvs tabularEnvs;


    // The default connection added to a data URI if it does not have it.
    protected Connection defaultConnection;


    /**
     * Connections
     */
    final Map<KeyNormalizer, Connection> connections;
    /**
     * Connections
     */
    final Map<KeyNormalizer, Service> services;


    /**
     * The exit status
     * We may want to show what we have an error but not throw it
     * Example: of all transfers, if only one has failed
     * we will gather the transfers data to create an output
     * and then fail
     */
    private int exitStatus = 0;


    /**
     * We may not add derived generated attributes.
     * so the key identifier is not a string
     */
    private final Map<TabularAttributeEnum, Attribute> attributes = new HashMap<>();
    private final DataUriBuilder dataUriBuilder;


    public Tabular(TabularConfig tabularConfig) {


        /**
         * Protector
         */
        String passphrase = TabularInit.determinePassphrase(tabularConfig.passphrase, attributes);
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
        tabularEnvs = new TabularEnvs(tabularConfig.envs, protector);

        /**
         * Vault
         */
        this.vault = Vault.create(protector, tabularEnvs);

        /**
         * Logs
         */
        TabularLogLevel logLevel = TabularInit.determineLogLevel(tabularConfig.logLevel, vault, tabularEnvs, attributes);

        TABULIFY_PACKAGE_LOGGER.setLevel(logLevel.getLevel());
        DbLoggers.LOGGER_TABULAR_START.info("The log level was set to " + logLevel);

        /**
         * Set the strict hood of the run
         *
         * @param strict - if the execution is strict or not
         */
        TabularInit.determineIsStrict(tabularConfig.isStrict, vault, attributes, tabularEnvs);

        /**
         * Project
         */
        this.appHomePath = TabularInit.determineProjectHome(tabularConfig.appHome, vault, attributes, tabularEnvs);
        if (appHomePath != null) {
            DbLoggers.LOGGER_TABULAR_START.info("This is a project run (" + appHomePath + ")");
        } else {
            DbLoggers.LOGGER_TABULAR_START.info("This is not a project run.");
        }

        Path userHomePath = TabularInit.determineUserHome(vault, tabularEnvs, attributes);
        if (tabularConfig.cleanEnv) {
            Fs.deleteIfExists(userHomePath, true);
        }

        /**
         * Execution Env
         */
        this.executionEnv = TabularInit.determineExecutionEnv(tabularConfig.execEnv, vault, tabularEnvs, attributes);

        /**
         * Home Path
         */
        Path tabulInstallationHomePath = TabularInit.determineHomePath(tabularConfig.homePath, executionEnv, tabularEnvs, attributes, vault);

        /**
         * Build tabular Connection
         * (After init of variables
         */
        Path osUserHome = Fs.getUserHome();
        // Hack to have a consistent os user home in the documentation
        String envValue = tabularEnvs.getOsEnvValue(KeyNormalizer.createSafe(TABUL_OS_USER_HOME));
        if (envValue != null) {
            osUserHome = Paths.get(envValue);
        }
        connections = ConnectionBuiltIn.loadBuiltInConnections(this, userHomePath, osUserHome, tabulInstallationHomePath);
        if (appHomePath != null) {
            addConnection(
                    Connection.createConnectionFromProviderOrDefault(this, ConnectionBuiltIn.APP_CONNECTION, appHomePath.toUri().toString())
                            .setComment("The project home path")
            );
            // Default connection is project
            this.setDefaultConnection(ConnectionBuiltIn.APP_CONNECTION);
        } else {
            // Default connection is cd
            this.setDefaultConnection(ConnectionBuiltIn.CD_LOCAL_FILE_SYSTEM);
        }

        /**
         * Tabul Yaml File
         * confPath is a field because it's used by the cli
         * to modify a conf file with the cli
         */
        ConfVault confVault;
        if (tabularConfig.loadConfigurationFile) {
            Path confPath = TabularInit.determineConfPath(tabularConfig.confPath, vault, tabularEnvs, appHomePath, userHomePath, attributes);
            confVault = ConfVault.createFromPath(confPath, this);
            if (!Files.exists(confPath)) {
                /**
                 * Why we create it?
                 * * We have in test env such as TABUL_CONNECTION_SMTP_TO and Tabular will report an error if it's not present
                 * * Easy tutorial, the users get them everytime
                 * * Easy doc, we just need to delete the file to get a clean environment
                 * * Easy maintenance, we don't need to recreate the environment each time
                 */
                DbLoggers.LOGGER_TABULAR_START.info("Conf path does not exist. Creating it " + confVault);
                confVault
                        .loadHowtoConnections()
                        .loadHowtoServices()
                        .flush();
            }
        } else {
            confVault = ConfVault.createEmpty(this);
        }


        /**
         * Other Tabular attributes, not processed
         */
        for (TabularAttributeEnum attribute : TabularAttributeEnum.class.getEnumConstants()) {

            if (attributes.containsKey(attribute)) {
                continue;
            }
            Vault.VariableBuilder variableBuilder = vault.createVariableBuilderFromAttribute(attribute);

            // Name
            // We don't look up without the tabul prefix because it can cause clashes
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
            envValue = tabularEnvs.getOsEnvValue(envName);
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
                            .setOrigin(Origin.DEFAULT)
                            .buildSafe(null)
            );

        }


        /**
         * Configuration vault connection and services
         */
        HashSet<Connection> confVaultConnections = confVault.getConnections();
        for (Connection connection : confVaultConnections) {
            this.addConnection(connection);
        }
        DbLoggers.LOGGER_TABULAR_START.info(confVault.getConnections().size() + " connections were loaded from the configuration vault");
        services = confVault.getServices();


        /**
         * Needed in test only when we want to test the {@link TabularConfig#addEnv(String, String)} setting of env}
         * on howto connections
         */
        if (tabularConfig.loadHowtoConnections) {
            loadHowtoConnections();
        }

        /**
         * Check for env
         */
        TabularInit.checkForEnvNotProcessed(tabularEnvs, attributes, connections, this);

        dataUriBuilder = DataUriBuilder.builder(this).build();

    }


    public static TabularConfig builder() {
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

        return Tabular
                .builder()
                .cleanEnv(true)
                .build();

    }

    public static Tabular tabularWithoutConfigurationFile() {

        return Tabular
                .builder()
                .loadConfigurationFile(false)
                .build();

    }

    public Tabular addConnection(Connection connection) {

        connections.put(connection.getName(), connection);
        return this;


    }


    public void setDefaultConnection(Connection connection) {
        this.defaultConnection = connection;
    }

    public void setDefaultConnection(KeyNormalizer connectionName) {
        Connection connection = getConnection(connectionName);

        if (connection == null) {
            throw new RuntimeException(
                    Strings.createMultiLineFromStrings("The connection (" + connectionName + ") was not found and could not be set as the default one.",
                            "The actual connections are (" + getConnections().stream().map(Connection::toString).collect(Collectors.joining(", ")) + ")").toString());
        }

        this.defaultConnection = connection;

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
                                .anyMatch(gp -> Glob.createOf(gp).matches(ds.getName().toString()))
                )
                .collect(Collectors.toList());
    }

    /**
     * @param uriOrDataUri - an uri or a data uri
     */
    public DataPath getDataPath(String uriOrDataUri) {
        return getDataPath(uriOrDataUri, null);
    }

    /**
     * @param uriOrDataUriString - an uri or a data uri
     * @param mediaType          as string (because the connection is not yet made)
     */
    public DataPath getDataPath(String uriOrDataUriString, MediaType mediaType) {

        DataUriNode dataUriObj = this.createDataUri(uriOrDataUriString);
        return getDataPath(dataUriObj, mediaType);

    }


    /**
     * @param dataUriNode - A data uri defining the first data path
     * @param mediaType   - The media type of the returned data path (or null if unknown, detected)
     * @return the data path
     */
    public DataPath getDataPath(DataUriNode dataUriNode, MediaType mediaType) {

        Connection dataUriConnection = dataUriNode.getConnection();

        String path;
        try {
            path = dataUriNode.getPath();
        } catch (NoPathFoundException e) {

            if (dataUriNode.isRuntime()) {
                DataUriNode executableDataUri = dataUriNode.getDataUri();
                DataPath executableDataPath = this.getDataPath(executableDataUri, null);
                /**
                 * Recursion
                 */
                while (executableDataPath.isRuntime()) {
                    executableDataPath = executableDataPath.execute();
                }
                return dataUriConnection.getRuntimeDataPath(executableDataPath, mediaType);
            }

            DataPath currentDataPath = dataUriConnection.getCurrentDataPath();
            if (mediaType != null && !MediaTypes.equals(currentDataPath.getMediaType(), mediaType)) {
                throw new InternalException("The asked media type (" + mediaType + ") is not the same as the media type of the returned resource (" + currentDataPath + "/ " + currentDataPath.getMediaType() + ")");
            }
            return currentDataPath;

        }


        return dataUriConnection.getDataPath(path, mediaType);


    }

    /**
     * @param dataUrString - a URL or a data URI String
     * @return the data uri object
     */
    public DataUriNode createDataUri(String dataUrString) {

        return dataUriBuilder.apply(dataUrString);

    }

    /**
     * @param connectionName - the name of the connection
     * @return a connection or null
     */
    public Connection getConnection(KeyNormalizer connectionName) {

        Objects.requireNonNull(connectionName, "The connection name is null. Internal error, you may want to create a qualified URI before to create the connection");

        return this.connections.get(connectionName);

    }

    /**
     * @param uri - the uri
     */
    public Connection createRuntimeConnection(String uri) {

        return createRuntimeConnection(uri, null);

    }


    /**
     * A runtime is a temporary connection (in-memory mostly)
     * <p>
     * We use it as namespace. For instance, a data generator needs to create a set of consistent resource (data path)
     * in-memory with foreign and unique keys
     * <p>
     * They are not added to the {@link #connections connections map}
     * because if we do, we then need to remove it in a complete/close step, and it adds a burden.
     * The Java  Garbage Collection engine can do that for us.
     * <p>
     * If you want to add it to {@link #connections connections map} for pipeline test for instance,
     * create the runtime connection, add it manually {@link Tabular#addConnection(Connection)}
     *
     * @param uri            - the uri of the connection
     * @param connectionName - the connection name (maybe null, derived from the uri)
     */
    public Connection createRuntimeConnection(String uri, KeyNormalizer connectionName) {

        if (connectionName == null) {
            try {
                /**
                 * We delete the .. and other strange characters
                 */
                String connectionNameNormalized = KeyNormalizer.create(uri).toCliLongOptionName();
                connectionName = KeyNormalizer.createSafe(connectionNameNormalized);
            } catch (CastException e) {
                throw new IllegalArgumentException("The uri (" + uri + ") cannot be used to create the connection name. Error: " + e.getMessage(), e);
            }
        }


        return Connection
                .createConnectionFromProviderOrDefault(this, connectionName, uri)
                .setOrigin(ObjectOrigin.RUNTIME);

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

        return (FsDataPath) getDataPath(path, null);

    }


    public List<DataPath> select(DataUriNode dataSelector) {
        return select(dataSelector, null);
    }

    public List<DataPath> select(String dataUriSelector) {

        return select(createDataUri(dataUriSelector), null);
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
    public List<DataPath> select(DataUriNode dataSelector, MediaType mediaType) {

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
        if (dataSelector.isRuntime()) {

            /**
             * The path part of the data uri defines a command or a query
             */
            List<DataPath> commandDataPaths = select(dataSelector.getDataUri(), null);
            for (DataPath commandDataPath : commandDataPaths) {
                dataPathsToReturn.add(connection.getRuntimeDataPath(commandDataPath, null));
            }

        } else {

            String pattern;
            try {
                pattern = dataSelector.getPath();
            } catch (NoPathFoundException e) {
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

        return (MemoryDataPath) getConnection(ConnectionBuiltIn.MEMORY_CONNECTION).getAndCreateRandomDataPath(null, null, null);

    }

    @SuppressWarnings("unused")
    public DataPath getAndCreateRandomDataPathWithType(MediaType mediaType) {

        return getConnection(ConnectionBuiltIn.MEMORY_CONNECTION).getAndCreateRandomDataPath(null, null, mediaType);

    }


    /**
     * Return if it's a strict run
     */
    public boolean isStrictExecution() {
        return (boolean) this.getAttribute(TabularAttributeEnum.STRICT_EXECUTION).getValueOrDefault();
    }


    /**
     * Utility function
     *
     * @return the memory datastore
     */
    public MemoryConnection getMemoryConnection() {
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
     * @param root  - the root directory (mandatory otherwise the root is not on the resources' directory)
     * @return the connection
     */
    public FsConnection createRuntimeConnectionForResources(Class<?> clazz, String root) {
        return createRuntimeConnectionForResources(clazz, root, null);
    }

    /**
     * @param clazz - the class
     * @param root  - the resource root directory with a root path (should start with /)
     * @param name  - the name of the created connection (maybe null)
     * @return the connection created
     * See {@link #getResourceDataPath(Class, String)}
     */
    public FsConnection createRuntimeConnectionForResources(Class<?> clazz, String root, KeyNormalizer name) {
        try {
            if (root.trim().equals("/")) {
                // The root path / is not fixed and is module dependent
                // ie Sql_71_SelectQueryViewTest.class.getResource("/") will yield the following results:
                // from the jdbc module: file:/home/admin/code/tabulify/tabulify-tabul-jdbc/target/test-classes/
                // from the sqlite module: file:/home/admin/code/tabulify/tabulify-sqlite/target/test-classes/
                // We forbid it then so that there is a search
                throw new InternalException("Root path / is forbidden");
            }
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
                name = KeyNormalizer.createSafe("resource_" + root);
            }
            Connection connection = this.getConnection(name);
            if (connection == null) {
                String url = uri.toString();
                connection = createRuntimeConnection(url, name);
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
     * @param msg - terminate if the run is strict or print a warning message
     */
    public void warningOrTerminateIfStrict(String msg) {
        if (this.isStrictExecution()) {
            DbLoggers.LOGGER_DB_ENGINE.warning("The run is strict, we terminate");
            throw new IllegalStateException(msg);
        }

        DbLoggers.LOGGER_DB_ENGINE.warning(msg);

    }

    /**
     * @param e Exception - terminate if the run is strict or print a warning message
     */
    public void warningOrTerminateIfStrict(Exception e) {
        if (this.isStrictExecution()) {
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


    public FsConnection getTmpConnection() {
        return (FsConnection) getConnection(ConnectionBuiltIn.TEMP_LOCAL_FILE_SYSTEM);
    }


    public <T> T getAttribute(TabularAttributeEnum attribute, Class<T> clazz) throws NoValueException, CastException, NoVariableException {
        Attribute variable = this.attributes.get(attribute);
        if (variable == null) {
            throw new NoValueException("The variable (" + attribute + ") was not found");
        }
        return variable.getValueOrDefaultCastAs(clazz);
    }


    public List<DataPath> select(List<DataUriNode> dataSelectors, boolean isStrictSelection, MediaType mediaType) {

        List<DataPath> dataPathSet = new ArrayList<>();

        for (DataUriNode dataUriSelector : dataSelectors) {

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
                if (isStrictSelection) {
                    throw new SelectionStrictException(msg);
                }

            } else {

                /**
                 * Glob
                 * Adding the glob backreference to the data path attributes
                 * ie 1, 2, 3
                 */
                boolean containsWildCard = dataUriSelector.isGlobPattern();
                if (containsWildCard) {

                    String pattern;
                    if (!dataUriSelector.isRuntime()) {
                        try {
                            pattern = dataUriSelector.getPath();
                        } catch (NoPathFoundException e) {
                            throw new InternalException("Not path found in (" + dataUriSelector + "). Should not happen as we test for a runtime", e);
                        }
                    } else {
                        try {
                            pattern = dataUriSelector.getDataUri().getPath();
                        } catch (NoPathFoundException e) {
                            throw new IllegalArgumentException("A pattern or path is mandatory with a script data uri");
                        }
                    }

                    for (DataPath dataPath : dataPathsByPattern) {
                        Connection connectionMatch;
                        /**
                         * The match is against the connection of the path
                         * (ie sqlite does not allow 2 names in the path but a file system allows it)
                         */
                        if (dataPath.isRuntime()) {
                            connectionMatch = dataPath.getExecutableDataPath().getConnection();
                        } else {
                            connectionMatch = dataPath.getConnection();
                        }

                        /**
                         * Relativize the pattern path if it's absolute
                         * (because the path of data path are relative by default)
                         */
                        if (connectionMatch instanceof FsConnection) {
                            String connectionPathString = ((FsConnection) connectionMatch).getCurrentDataPath().getNioPath().toAbsolutePath() + "/";
                            if (pattern.startsWith(connectionPathString)) {
                                pattern = pattern.substring(connectionPathString.length());
                            }
                        }

                        /**
                         * Glob
                         */
                        Glob glob = connectionMatch
                                .createStringPath(pattern)
                                .standardize()
                                .toGlobExpression();
                        /**
                         * The match is against the compact name
                         */
                        String compactName;
                        if (dataPath.isRuntime()) {
                            compactName = dataPath.getExecutableDataPath().getCompactPath();
                        } else {
                            compactName = dataPath.getCompactPath();
                        }
                        List<String> groups = glob.getGroups(compactName);
                        // We start from 0 for backwards compatibility, ($0) being the whole name
                        int start = 0;
                        for (int i = start; i < groups.size(); i++) {
                            KeyNormalizer key = KeyNormalizer.createSafe(String.valueOf(i));
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


    public FsConnection createRuntimeConnectionFromLocalPath(KeyNormalizer connectionName, Path localPath) {
        String uri = localPath.toAbsolutePath()
                .normalize()
                .toUri()
                .toString();
        return (FsConnection) this.createRuntimeConnection(uri, connectionName);
    }

    public FsConnection createRuntimeConnectionFromLocalPath(String connectionName, Path localPath) {

        try {
            return createRuntimeConnectionFromLocalPath(KeyNormalizer.create(connectionName), localPath);
        } catch (CastException e) {
            throw new IllegalArgumentException("The connection name (" + connectionName + ") is not conform. Error: " + e.getMessage(), e);
        }
    }


    @SuppressWarnings("unused")
    public FsDataPath getTempDirectory(String prefix) {
        return (FsDataPath) this.getTmpConnection().getDataPath("tabli" + prefix + UUID.randomUUID());
    }


    public boolean isProjectRun() {
        return this.appHomePath != null;
    }


    /**
     * The execution environment
     */
    public TabularExecEnv getExecutionEnvironment() {

        return this.executionEnv;
    }


    public FsDataPath getTempFile(String prefix, String suffix) {
        return (FsDataPath) this.getTmpConnection().getDataPath(prefix + UUID.randomUUID() + suffix);
    }

    public DataUriNode getDefaultUri() {
        return DataUriNode
                .builder()
                .setConnection(this.getDefaultConnection())
                .setPath("")
                .build();
    }


    public Vault getVault() {
        return this.vault;
    }

    @SuppressWarnings("unused")
    public void removeConnection(KeyNormalizer connectionName) {
        Connection conn = this.connections.remove(connectionName);
        conn.close();
    }

    public String getName() {
        return TABUL_NAME;
    }


    public Attribute getAttribute(TabularAttributeEnum attribute) {
        return this.attributes.get(attribute);
    }


    public MemoryDataPath createMemoryDataPath(String path) {
        return this.getMemoryConnection().getDataPath(path);
    }

    @SuppressWarnings("UnusedReturnValue")
    public Tabular addConnection(KeyNormalizer name, Connection connection) {
        this.connections.put(name, connection);
        return this;
    }


    /**
     * Load the howto connection in this tabular
     * (used in test mostly)
     */
    public Tabular loadHowtoConnections() {

        this.connections.putAll(
                Howtos
                        .getConnections(this)
                        .stream()
                        .collect(Collectors.toMap(
                                Connection::getName,
                                connection -> connection
                        )));
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


    public Path getConfPath() {
        return (Path) this.getAttribute(TabularAttributeEnum.CONF).getValueOrDefault();
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

    public Path getTabliUserHome() {
        return (Path) this.getAttribute(TabularAttributeEnum.USER_HOME).getValueOrDefault();
    }


    public Tabular loadHowtoServices() {

        this.services.putAll(
                Howtos.getServices(this)
                        .stream()
                        .collect(Collectors.toMap(
                                Service::getName,
                                service -> service
                        )));
        return this;
    }

    public Set<Service> getServices() {
        return new HashSet<>(this.services.values());
    }

    public Service getService(KeyNormalizer name) {
        return services.get(name);
    }


    public String getApplicationName() {
        return TABUL_NAME;
    }

    public FsConnection getDataHomeDirectory() {
        return (FsConnection) getConnection(ConnectionBuiltIn.DATA_HOME_LOCAL_FILE_SYSTEM);
    }

    public DataUriNode createDataUri(DataUriStringNode dataUriStringNode) {
        return dataUriBuilder.apply(dataUriStringNode);
    }

    /**
     * A utility class to get a data path from the resources directory
     *
     * @param clazz        - the clazz
     * @param relativePath - the relative path from the resources directory
     * @return the data path
     */
    public DataPath getResourceDataPath(Class<?> clazz, String relativePath) {
        KeyNormalizer connectionName = KeyNormalizer.createSafe("resources");
        Path path = Paths.get(relativePath);
        return createRuntimeConnectionForResources(clazz, path.getParent().toString(), connectionName).getDataPath(path.getFileName().toString());
    }

    public Level getLogLevel() {
        return TABULIFY_PACKAGE_LOGGER.getLevel();
    }


    public FsDataPath getDataPath(Path path, MediaType mediaType) {
        DataUriNode dataUri = dataUriBuilder.apply(path.toUri());
        try {
            return (FsDataPath) dataUri.getConnection().getDataPath(dataUri.getPath(), mediaType);
        } catch (NoPathFoundException e) {
            throw new InternalException("The path should be available as we give it");
        }
    }

    public List<DataPath> select(DataUriStringNode dataUriSelector) {
        return select(createDataUri(dataUriSelector));
    }

    /**
     * Return a doc link
     */
    public String getDocLink(String docId) {
        if (this.isIdeEnv()) {
            return "http://localhost:8081/" + docId;
        }
        return "https://www.tabulify.com/" + docId;
    }


    public static class TabularConfig {
        private Path homePath;
        private String passphrase;
        private Path appHome;
        private Path confPath;
        private TabularExecEnv execEnv;
        private final Map<String, String> envs = new HashMap<>();
        private TabularLogLevel logLevel;
        // Do we need to clean the env (ie delete the user home directory)
        private boolean cleanEnv = false;
        private Boolean isStrict;
        private boolean loadHowtoConnections = false;
        /**
         * Used mostly in test
         * Do we load a conf file?
         */
        private boolean loadConfigurationFile = true;

        public TabularConfig setPassphrase(String passphrase) {
            this.passphrase = passphrase;
            return this;
        }

        public TabularConfig setAppHome(Path appHome) {
            this.appHome = appHome;
            return this;
        }

        public TabularConfig setConf(Path confPath) {
            this.confPath = confPath;
            return this;
        }

        /**
         * Add an env (to simulate an os env)
         */
        public TabularConfig addEnv(String key, String value) {
            this.envs.put(key, value);
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

        /**
         * Do we need to clean the env (ie delete the user home directory)
         */
        public TabularConfig cleanEnv(boolean clean) {
            this.cleanEnv = clean;
            return this;
        }

        public TabularConfig setStrictExecution(Boolean isStrict) {
            this.isStrict = isStrict;
            return this;
        }

        /**
         * Load the howto connections
         * This is needed if connection env are set {@link #addEnv(String, String)}
         * Otherwise the connection is unknown
         */
        public TabularConfig loadHowtoConnections() {
            this.loadHowtoConnections = true;
            return this;
        }

        public TabularConfig loadConfigurationFile(boolean loadConfigurationFile) {
            this.loadConfigurationFile = loadConfigurationFile;
            return this;
        }


    }
}
