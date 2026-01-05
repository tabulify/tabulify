package com.tabulify.connection;

import com.tabulify.DbLoggers;
import com.tabulify.Tabular;
import com.tabulify.Vault;
import com.tabulify.conf.*;
import com.tabulify.fs.FsConnection;
import com.tabulify.memory.MemoryDataPath;
import com.tabulify.model.*;
import com.tabulify.noop.NoOpConnection;
import com.tabulify.spi.*;
import com.tabulify.uri.DataUriNode;
import com.tabulify.uri.DataUriStringNode;
import com.tabulify.exception.*;
import com.tabulify.type.*;

import java.net.URI;
import java.nio.file.FileSystems;
import java.sql.DriverManager;
import java.util.*;
import java.util.stream.Collectors;

import static com.tabulify.conf.Origin.DEFAULT;

/**
 * A connection
 */
public abstract class Connection implements Comparable<Connection>, AutoCloseable {

    public static final String ALIAS_SCHEME = "alias";

    public static final String CONNECTION = "connection";

    private final Tabular tabular;

    private final Random random = new Random();
    private final Vault vault;
    protected final SqlDataTypeManager sqlDataTypeManager;


    /**
     * @param tabular - the global context
     * @param name    - the connection name (Why an attribute, no particular reason, what fucked up is fucked up, except that we will have it in the attributes)
     * @param uri     -  the URI attribute (it's an attribute and not a URI because it may change by environment)
     *                Example: Sqlite URI is not the same as the path is not the same on Linux and Windows
     */
    public Connection(Tabular tabular, Attribute name, Attribute uri) {

        this.tabular = tabular;
        this.vault = tabular.getVault();

        try {
            // name check
            name.getValue();
        } catch (NoValueException e) {
            throw new InternalException("A connection cannot be created without name");
        }

        /**
         * We can't add them via addVariable
         * because they are immutable
         */
        this.attributes.put((ConnectionAttributeEnum) uri.getAttributeMetadata(), uri);
        this.attributes.put((ConnectionAttributeEnum) name.getAttributeMetadata(), name);

        this.addAttributesFromEnumAttributeClass(ConnectionAttributeEnumBase.class);

        /**
         * Init
         */
        this.sqlDataTypeManager = new SqlDataTypeManager(this);


    }


    /**
     * Connection Variable. Variable managed by Tabul
     * Should be a known attribute
     */
    Map<ConnectionAttributeEnum, Attribute> attributes = new HashMap<>();
    /**
     * Driver Variable. Variable of the driver/library, not from us
     * We use attribute to add vault functionality
     * String is the original property
     */
    Map<String, Attribute> nativeDriverAttributes = new HashMap<>();


    /**
     * Deep copy
     *
     * @param connection the origin connection
     * @return a new reference
     */
    public static Connection of(Connection connection) {
        return Connection.createConnectionFromProviderOrDefault(connection.getTabular(), connection.getNameAsAttribute(), connection.getUriAsVariable())
                .setAttributes(connection.getAttributes());

    }


    public Attribute getDescription() {
        return this.attributes.get(ConnectionAttributeEnumBase.COMMENT);
    }


    public KeyNormalizer getName() {

        try {
            return (KeyNormalizer) getNameAsAttribute().getValue();
        } catch (NoValueException e) {
            throw new InternalException("It should not happen as name is mandatory");
        }

    }

    public com.tabulify.conf.Attribute getNameAsAttribute() {

        return this.getAttribute(ConnectionAttributeEnumBase.NAME);
    }

    public com.tabulify.conf.Attribute getUriAsVariable() {

        return this.getAttribute(ConnectionAttributeEnumBase.URI);

    }


    @Override
    public String toString() {
        /**
         * We don't normalize connection name
         * If a user enter (myConnection, it will not expect my-connection)
         */
        return this.getName().toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Connection connection = (Connection) o;
        return Objects.equals(this.getName(), connection.getName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getName());
    }

    public Connection setUser(String user) {
        try {
            com.tabulify.conf.Attribute userAttribute = tabular.getVault().createAttribute(ConnectionAttributeEnumBase.USER, user, DEFAULT);
            this.addAttribute(userAttribute);
        } catch (Exception e) {
            throw new RuntimeException("Error while creating the user variable", e);
        }
        return this;
    }

    public Connection setPassword(String pwd) {
        try {
            Attribute password = tabular.getVault().createAttribute(ConnectionAttributeEnumBase.PASSWORD, pwd, DEFAULT);
            this.attributes.put(ConnectionAttributeEnumBase.PASSWORD, password);
        } catch (Exception e) {
            throw new RuntimeException("Error while creating the password variable for the connection (" + this + "). Error: " + e.getMessage(), e);
        }
        return this;
    }


    public Attribute getUser() {
        return this.attributes.get(ConnectionAttributeEnumBase.USER);
    }

    public String getPassword() {
        return (String) this.attributes.get(ConnectionAttributeEnumBase.PASSWORD).getValueOrDefault();
    }

    @Override
    public int compareTo(Connection o) {

        return this.getName().compareTo(o.getName());

    }

    /**
     * @return the scheme of the data store
     * * file
     * * ...
     */
    public String getScheme() {

        return getUri().getScheme();

    }


    /**
     * Add a free form key
     * Each connection should implement it to add its own attribute
     * and call super to add the attribute of its parent if the name is unknown
     */
    public Connection addAttribute(KeyNormalizer name, Object value, Origin origin) {
        ConnectionAttributeEnumBase connectionAttributeBase;
        try {
            connectionAttributeBase = Casts.cast(name, ConnectionAttributeEnumBase.class);
        } catch (CastException e) {
            List<Class<? extends AttributeEnumParameter>> connectionEnumParameters = this.getAttributeEnums().stream().map(enumClass -> (Class<? extends AttributeEnumParameter>) enumClass).collect(Collectors.toList());
            throw new RuntimeException("The connection attribute (" + name + ") is unknown for the connection " + this + ". We were expecting one of the following " + tabular.toPublicListOfParameters(connectionEnumParameters), e);
        }
        try {
            com.tabulify.conf.Attribute attribute = vault.createAttribute(connectionAttributeBase, value, origin);
            this.addAttribute(attribute);
        } catch (Exception e) {
            throw new RuntimeException("Error while adding the variable " + name + " to the connection " + this + ". Error: " + e.getMessage(), e);
        }
        return this;
    }

    /**
     * @return the list of enum class
     * Typically, a connection would add its own class
     * This is used to give feedback when an attribute is not recognized when reading a {@link ConfVault config file}
     */
    public List<Class<? extends ConnectionAttributeEnum>> getAttributeEnums() {
        return List.of(ConnectionAttributeEnumBase.class);
    }


    /**
     * Shortcut for code
     * <code>
     * connection.addAttribute(vault.createAttribute(Attribute, Origin.CONF, Origin.DEFAULT))
     * </code>
     */
    public Connection addAttribute(ConnectionAttributeEnum key, Object value, Origin origin) {
        try {
            com.tabulify.conf.Attribute attribute = vault.createAttribute(key, value, origin);
            this.addAttribute(attribute);
        } catch (Exception e) {
            throw new RuntimeException("Error while adding connection the property " + key + ". Error: " + e.getMessage(), e);
        }
        return this;
    }

    public Connection addAttribute(Attribute attribute) {
        AttributeEnum attributeEnum = attribute.getAttributeMetadata();
        if (!(attributeEnum instanceof ConnectionAttributeEnum)) {
            throw new InternalException("The attribute " + attributeEnum + " is not a connection attribute but a " + attributeEnum.getClass().getSimpleName());
        }
        ConnectionAttributeEnum connectionAttribute = (ConnectionAttributeEnum) attributeEnum;
        // uri and name value cannot be changed as they are constructor variable
        // the original may be template so we allow to change the variable if the value are the same
        if (attributeEnum.equals(ConnectionAttributeEnumBase.NAME) && !attribute.getValueOrDefault().equals(this.getName())) {
            throw new RuntimeException("You can't change the name of this connection from " + this.getName() + " to " + attribute.getValueOrDefaultAsStringNotNull());
        }
        if (attributeEnum.equals(ConnectionAttributeEnumBase.URI) && !attribute.getValueOrNull().equals(this.getUri())) {
            throw new RuntimeException("You can't change the URI of this connection from " + this.getUri() + " to  " + attribute.getValueOrDefaultAsStringNotNull());
        }
        Attribute actualAttribute = attributes.get(connectionAttribute);
        if (actualAttribute != null) {
            // overwrite of an actual known attribute
            // we copy the attribute otherwise the description is lost
            attribute.setAttributeMetadata(actualAttribute.getAttributeMetadata());
        }
        attributes.put(connectionAttribute, attribute);
        return this;
    }

    public static Connection createConnectionFromProviderOrDefault(Tabular tabular, KeyNormalizer connectionName, String uri) {

        try {
            return createConnectionFromProviderOrDefault(
                    tabular,
                    tabular.getVault().createAttribute(ConnectionAttributeEnumBase.NAME, connectionName, DEFAULT),
                    tabular.getVault().createAttribute(ConnectionAttributeEnumBase.URI, uri, DEFAULT)
            );
        } catch (Exception e) {
            throw ExceptionWrapper.builder(e, "Error while creating the connection (" + connectionName + "," + uri + ").")
                    .setPosition(ExceptionWrapper.ContextPosition.FIRST)
                    .buildAsRuntimeException();
        }
    }

    public static Connection createConnectionFromProviderOrDefault(Tabular tabular, Attribute attributeName, Attribute attributeUri) {

        UriEnhanced uri = (UriEnhanced) attributeUri.getValueOrDefault();
        if (uri.getScheme() != null && uri.getScheme().equals(ALIAS_SCHEME)) {
            String schemeSpecificPart = uri.getSchemeSpecificPart();
            DataUriStringNode dataUriString;
            try {
                dataUriString = DataUriStringNode.createFromString(schemeSpecificPart);
            } catch (CastException e) {
                throw new IllegalArgumentException("The scheme (" + schemeSpecificPart + ") of the uri (" + uri + ") is not valid data uri string. Error: " + e.getMessage(), e);
            }
            KeyNormalizer connectionName = dataUriString.getConnection();
            Connection connection = tabular.getConnection(connectionName);
            if (connection == null) {
                String message = "The connection is unknown.";
                if (connectionName.equals(ConnectionBuiltIn.APP_CONNECTION)) {
                    message = "Tabulify does not have detected any app.";
                }
                message += " In the alias uri (" + uri + "), the connection (" + connectionName + ") is unknown.";
                throw new IllegalArgumentException(message);
            }
            if (connection.getClass() != FsConnection.class) {
                throw new IllegalArgumentException("The " + ALIAS_SCHEME + " scheme is supported only with file system data uri. The connection " + connectionName + " is not a file system connection but a " + connection.getClass().getSimpleName() + " connection");
            }
            String resolve = connection.getCurrentDataPath().resolve(dataUriString.getPath()).getAbsolutePath();
            attributeUri.setPlainValue("file://" + resolve.replace("\\", "/"));
        }

        List<ConnectionProvider> installedProviders = ConnectionProvider.installedProviders();
        for (ConnectionProvider connectionProvider : installedProviders) {
            if (connectionProvider.accept(attributeUri)) {
                return connectionProvider.createConnection(tabular, attributeName, attributeUri);
            }
        }

        // No provider was found
        final String message = "No provider was found for the connection (" + attributeName.getValueOrDefault() + ") with the Uri (" + attributeUri.getValueOrDefault() + "). Defaulting to the no-operation connection.";
        DbLoggers.LOGGER_DB_ENGINE.warning(message);
        return new NoOpConnection(tabular, attributeName, attributeUri);

    }


    public abstract DataSystem getDataSystem();


    /**
     * This is the main public function to get a data path
     *
     * @param pathOrName - the pathOrName
     *                   * a {@link ResourcePath string path } if the second argument names is null,
     *                   * otherwise a {@link ResourcePath#getNames() name part} of the path string
     * @param mediaType  - the media type or null for automatic detection or default
     * @return a data path
     * * from the current working directory
     * * if the first string is not an absolute path string
     * * if names is not null
     * * absolute if the first part is absolute and names is null
     * <p>
     * You can use special characters such as {@link #getCurrentPathCharacters() current name (the current working dir)}
     * and {@link #getParentPathCharacters() parent characters}
     */
    public abstract DataPath getDataPath(String pathOrName, MediaType mediaType);


    /**
     * @param pathOrName a path
     * @param mediaType  a media type not null
     */
    public DataPath getDataPath(String pathOrName, String mediaType) {
        try {
            return getDataPath(pathOrName, MediaTypes.parse(mediaType));
        } catch (NullValueException e) {
            throw new RuntimeException("A media type should not be null or empty", e);
        }
    }


    public abstract DataPath getDataPath(String pathOrName);

    /**
     * @return the name in a {@link ResourcePath} that represents the current working container
     * <p>
     * This is not a path but only a name (part)
     * <p>
     * ie
     * * working directory for a file system (`.`)
     * * or the schema for a sql database (`/`)
     */
    public abstract String getCurrentPathCharacters();

    /**
     * @return the name in a {@link ResourcePath} that represents a parent container (ie a parent directory)
     * <p>
     * This is not a path but only a name (part)
     * <p>
     * ie
     * * for a file system (`..`)
     */
    public abstract String getParentPathCharacters();


    /**
     * @return the {@link ResourcePath} separator (ie / or \ or . - system dependent)
     */
    public abstract String getSeparator();

    /**
     * @return the current/working path of this connection
     * return null if not supported (ie {@link NoOpConnection} or smtp
     */
    public abstract DataPath getCurrentDataPath();

    @Override
    public void close() {
        // Nothing to do here
    }


    public Set<Attribute> getAttributes() {

        return new HashSet<>(this.attributes.values());

    }


    public Connection setAttributes(Set<Attribute> attributes) {
        attributes.forEach(this::addAttribute);
        return this;
    }

    /**
     * Return true if this data store is open
     * <p>
     * This is to prevent a close error when a data store is:
     * * not used
     * * in the list
     * * its data system is not in the classpath
     * <p>
     * Example: the file datastore will have no provider when developing the db-jdbc module
     *
     * @return true if this data system was build
     */
    public boolean isOpen() {
        return false;
    }


    /**
     * Comment and not description because this is the name of the description column in a relational db
     */
    public Connection setComment(String description) {
        com.tabulify.conf.Attribute descVar;
        try {
            descVar = tabular.getVault().createAttribute(ConnectionAttributeEnumBase.COMMENT, description, DEFAULT);
        } catch (Exception e) {
            throw new RuntimeException("Internal error, cannot create description variable", e);
        }
        this.addAttribute(descVar);
        return this;
    }

    /**
     * Select from the {@link #getCurrentDataPath()} data resources
     * from a glob path define by string or by names
     *
     * @param globPathOrName a glob path or a name
     * @param mediaType      - the media Type
     * @param <D>            a type that extends data path
     * @return a list of data path
     */
    public <D extends DataPath> List<D> select(String globPathOrName, MediaType mediaType) {

        try {
            DataSystem dataSystem = getDataSystem();
            return dataSystem.select(getCurrentDataPath(), globPathOrName, mediaType);
        } catch (Exception e) {
            throw new RuntimeException("Error while selecting the data paths with the selector (" + globPathOrName + ") and media type (" + mediaType + ") for the connection (" + this + "). Error:" + e.getMessage(), e);
        }

    }


    public <D extends DataPath> List<D> select(String globPathOrName) {
        return select(globPathOrName, null);
    }


    public ConnectionMetadata getMetadata() {
        return new ConnectionMetadata(this);
    }


    /**
     * @param executableDataPath - the executable data path
     * @param mediaType          - the media type of the returned data path
     */
    public abstract DataPath getRuntimeDataPath(DataPath executableDataPath, MediaType mediaType);

    /**
     * @return all data types
     */
    public Set<SqlDataType<?>> getSqlDataTypes() {

        return this.sqlDataTypeManager.getSqlDataTypes();

    }

    /**
     * Utility function to create an executable from a content
     *
     * @param code - the code
     * @return an executable
     */
    public DataPath getRuntimeDataPath(String code) {

        MemoryDataPath dataPath = this.getTabular()
                .getAndCreateRandomMemoryDataPath()
                .setContent(code);
        return getRuntimeDataPath(dataPath, null);

    }


    /**
     * @deprecated set/add a new SqlDataTpe with {@link DataSystem#dataTypeBuildingMain(SqlDataTypeManager)} to overwrite the default
     * with the data of the driver or from the documentation
     */
    @Deprecated
    public SqlDataType<?> getDefaultSqlDataType(Integer typeCode) {

        return this.sqlDataTypeManager.getSqlDataType(typeCode);

    }

    /**
     * @param typeName - the type name
     * @param typeCode - the type code
     * @return the data type from a sql type name
     */
    public SqlDataType<?> getSqlDataType(KeyNormalizer typeName, int typeCode) {

        return this.sqlDataTypeManager.getSqlDataType(typeName, typeCode);
    }

    public SqlDataType<?> getSqlDataType(String typeName, int typeCode) {

        return this.sqlDataTypeManager.getSqlDataType(KeyNormalizer.createSafe(typeName), typeCode);

    }

    public SqlDataType<?> getSqlDataType(KeyNormalizer typeName, SqlDataTypeAnsi ansiType) {

        if (ansiType == null) {
            return this.sqlDataTypeManager.getSqlDataType(typeName);
        }
        return this.sqlDataTypeManager.getSqlDataType(typeName, ansiType.getVendorTypeNumber());

    }


    /**
     * @param clazz - the java class
     * @return @return the sql data type for a java class
     * This is not a bidirectional function with {@link SqlDataType#getClass()}
     * * The class of the SQL Data Type are the class needed by the driver to load the data type
     * * The class below is just a java class.
     */
    public SqlDataType<?> getSqlDataType(Class<?> clazz) {

        return this.sqlDataTypeManager.getSqlDataType(clazz);

    }

    public abstract ProcessingEngine getProcessingEngine();

    /**
     * Short Utility to call {@link #getAndCreateRandomDataPath(String, String, MediaType)}
     * with only the prefix
     */
    public DataPath getAndCreateRandomDataPath(String prefix) {
        return getAndCreateRandomDataPath(prefix, null, null);
    }

    /**
     * Sort of temporary data path
     *
     * @return a default typed data path with a UUID v4 name
     */
    public DataPath getAndCreateRandomDataPath(String prefix, String suffix, MediaType mediaType) {
        // We take only the first characters to only have alphabetic characters, no letter minus and
        // character that SQL or other system would not take
        StringBuilder name = new StringBuilder();
        // prefix
        if (prefix != null) {
            name.append(prefix);
        }
        // Hack, Sql name should start with a letter
        // It's a hack because name is for now immutable
        name.append((char) ('a' + random.nextInt(26)));
        // uuid part
        name.append(UUID.randomUUID().toString(), 0, 8);
        // suffix, generally extension
        if (suffix != null) {
            name.append(suffix);
        }
        return getDataPath(name.toString(), mediaType);
    }

    public DataPath getAndCreateRandomDataPath() {
        return getAndCreateRandomDataPath(null, null, null);
    }

    /**
     * Transform a value object into the desired clazz
     * with our cast system
     * <p>
     * For instance, this function is used in sql
     * in select stream, to take over the {@link java.sql.ResultSet#getObject(String, Class)}
     * in insert stream, to transform `yes` to `true` for a boolean
     *
     * @param valueObject - the value object created by the connection that that should be cast
     * @param clazz       - the class
     * @param <T>         - the t
     * @return the object cast
     */
    public <T> T getObject(Object valueObject, Class<T> clazz) throws CastException {
        return Casts.cast(valueObject, clazz);
    }

    /**
     * @return the {@link Tabular object}
     */
    public Tabular getTabular() {
        return tabular;
    }


    /**
     * Utility to obtain a {@link ResourcePath string path}
     *
     * @param pathOrName - if names is null, this is path otherwise it's considered as a path name
     * @param names      - other names (utility to give a path without knowing the separator)
     * @return a string path object to manipulate the string version of the path of a {@link DataUriNode#getPath()}
     */
    public ResourcePath createStringPath(String pathOrName, String... names) {

        return new ConnectionResourcePathBase(pathOrName);

    }

    /**
     * Two connection may point to the same service
     * with two different current directory
     * <p>
     * This is the case with connection that:
     * * uses the local system
     * * but as two different local directory connection
     * <p>
     * When the service id is the same, the transfer will transfer the file
     * using remote command to the service (ie rename mostly) and not using pull and push.
     *
     * @return the service id
     */
    public String getServiceId() {
        return getUri().toString();
    }


    /**
     * Translate a sql data type from one connection to another
     *
     * @param columnDef the source column def to get the data type and the column precision/scale
     * @return the data type in the target connection
     */
    public SqlDataType<?> getSqlDataTypeFromSourceColumn(ColumnDef<?> columnDef) {

        SqlDataType<?> sourceSqlDataType = columnDef.getDataType();
        if (sourceSqlDataType.getConnection().equals(this)) {
            return sourceSqlDataType;
        }

        /**
         * Translation based on name
         */
        SqlDataType<?> translatedType = this.getSqlDataType(sourceSqlDataType.toKeyNormalizer(), sourceSqlDataType.getVendorTypeNumber());
        if (translatedType != null) {
            // name is not enough
            // for instance,
            // * timestamp in sql server is a binary
            // * int identity in sql server is an integer but autoincrement
            if (
                    translatedType.getAnsiType() == sourceSqlDataType.getAnsiType() &&
                            translatedType.getAutoIncrement() == sourceSqlDataType.getAutoIncrement() &&
                            translatedType.getUnsignedAttribute() == sourceSqlDataType.getUnsignedAttribute()
            ) {
                return translatedType;
            }
        }

        /**
         * Unsigned Integer
         */
        if (sourceSqlDataType.isInteger() && sourceSqlDataType.getUnsignedAttribute()) {
            SqlDataType<?> targetType = this.sqlDataTypeManager.getSqlDataTypes()
                    .stream()
                    .filter(d -> d.getUnsignedAttribute() && d.getAnsiType() == sourceSqlDataType.getAnsiType())
                    .findFirst()
                    .orElse(null);
            if (targetType != null) {
                return targetType;
            }
            // The database does not support unsigned type
            // taking the next integer in precision order
            try {
                return this.sqlDataTypeManager.getUpperUnsignedIntegerType(sourceSqlDataType.getAnsiType());
            } catch (CastException e) {
                throw new RuntimeException("We couldn't convert the unsigned integer data type " + sourceSqlDataType + " from the column (" + columnDef + ") to the target system (" + this + "). Error:" + e.getMessage(), e);
            }
        }

        /**
         * Translation based on ansi type
         * (Note: We should have more filter based on precision and scale for number)
         */
        translatedType = this.getSqlDataType(sourceSqlDataType.getAnsiType());
        if (translatedType != null) {
            return translatedType;
        }

        /**
         * On Class
         */
        translatedType = this.getSqlDataType(sourceSqlDataType.getValueClass());
        if (translatedType != null) {
            return translatedType;
        }

        /**
         * It can happen on custom tabulify added sql type
         * with -100 and below
         */
        return sourceSqlDataType;
    }


    /**
     * @return true if the connection was pinged
     */
    public abstract Boolean ping();

    public Connection setOrigin(ObjectOrigin connectionOrigin) {
        this.getAttribute(ConnectionAttributeEnumBase.ORIGIN)
                .setPlainValue(connectionOrigin);
        return this;
    }

    public ObjectOrigin getOrigin() {
        try {
            return (ObjectOrigin) this.getAttribute(ConnectionAttributeEnumBase.ORIGIN).getValue();
        } catch (NoValueException e) {
            throw new InternalException("No Origin found", e);
        }

    }


    /**
     * Uri can contain characters such as /
     * that are not allowed in connection name
     * <p>
     * `/` are not supported because in ini file they may define a hierarchy and create then several datastore
     * <a href="http://ini4j.sourceforge.net/tutorial/IniTutorial.java.html">...</a>
     * <p>
     * This function helps to get a consistent naming
     * from an uri
     * <p>
     * This is normally for temporary data store
     * that has little impact
     *
     * @param uri - the URI
     * @return a valid connection name
     */
    public static KeyNormalizer getConnectionNameFromUri(URI uri) {
        String scheme = uri.getScheme();
        String host = uri.getHost();
        String name = "";
        if (host != null) {
            name = host;
        }
        String finalValue = scheme + "-" + (name + uri.getPath()).replace("/", "\\");
        try {
            return KeyNormalizer.create(finalValue);
        } catch (CastException e) {
            throw new RuntimeException("The derived connection name (" + finalValue + ") for the URI (" + uri + ") is not valid. Error: " + e.getMessage(), e);
        }
    }


    public com.tabulify.conf.Attribute getAttribute(ConnectionAttributeEnum attribute) {
        com.tabulify.conf.Attribute variable = this.attributes.get(attribute);
        if (variable == null) {
            /**
             * connection attribute should be present
             * added via {@link #addAttributesFromEnumAttributeClass(Class)} in the constructor
             */
            throw new RuntimeException("Internal: The connection attribute " + attribute + " was not found. Did you add it at construction time?");
        }
        return variable;
    }

    /**
     * A utility class to add the default variables when a connection is build
     *
     * @param enumClass - the class that holds all enum attribute
     * @return the path for chaining
     */
    public Connection addAttributesFromEnumAttributeClass(Class<? extends ConnectionAttributeEnum> enumClass) {

        UriEnhanced connectionUri = this.getUri();

        for (ConnectionAttributeEnum attribute : enumClass.getEnumConstants()) {

            Vault vault = this.tabular.getVault();
            Vault.VariableBuilder variableBuilder = vault.createVariableBuilderFromAttribute(attribute);

            // What is fucked up, is fucked up
            // Name and uri are constructor variable and are therefore already added
            if (attribute == ConnectionAttributeEnumBase.NAME || attribute == ConnectionAttributeEnumBase.URI) {
                continue;
            }

            // Query Properties

            String prop = connectionUri.getQueryProperty(attribute.toString());
            if (prop != null && !prop.trim().isEmpty()) {
                com.tabulify.conf.Attribute variable = variableBuilder
                        .setOrigin(com.tabulify.conf.Origin.URI)
                        .buildSafe(prop);
                this.addAttribute(variable);
                continue;
            }

            // Env
            // 20251208 - Deprecated the env name relation is done in the manifest as value
            // We don't look up without the tabul prefix because it can cause clashes
            // for instance, name in os is the name of the computer
            //TabularEnvs tabularEnvs = this.tabular.getTabularEnvs();
            //KeyNormalizer envName = KeyNormalizer.createSafe(Tabular.TABUL_NAME + "_" + OsEnvType.CONNECTION + "_" + this.getName() + "_" + attribute);
            //String envValue = tabularEnvs.getOsEnvValue(envName);
            //if (envValue != null) {
            //    com.tabulify.conf.Attribute variable = variableBuilder
            //            .setOrigin(com.tabulify.conf.Origin.OS)
            //            .buildSafe(envValue);
            //    this.addAttribute(variable);
            //    continue;
            //}

            if (attribute == ConnectionAttributeEnumBase.WORKING_PATH) {
                com.tabulify.conf.Attribute variable = variableBuilder
                        .setOrigin(DEFAULT)
                        .build(() -> {
                                    DataPath currentDataPath = this.getCurrentDataPath();
                                    if (currentDataPath == null) {
                                        return "";
                                    }
                                    return currentDataPath.getAbsolutePath();
                                }
                                , String.class);
                this.addAttribute(variable);
                continue;
            }


            // None
            com.tabulify.conf.Attribute variable = variableBuilder
                    .setOrigin(Origin.DEFAULT)
                    .buildSafe(null);
            this.addAttribute(variable);

        }
        return this;
    }

    public UriEnhanced getUri() {

        try {
            return (UriEnhanced) getUriAsVariable().getValue();
        } catch (NoValueException e) {
            throw new InternalException("Uri was not found in the connection (" + this + ") but is mandatory for construction");
        }

    }


    public Map<String, Attribute> getNativeDriverAttributes() {
        return this.nativeDriverAttributes;
    }


    /**
     * @param vault - vault is there to be able to pass the vault from {@link ConfVault}
     */
    public void addNativeAttribute(String name, String value, Origin origin, Vault vault) {
        Attribute nativeAttribute;
        try {
            nativeAttribute = vault.createAttribute(name, value, origin);
        } catch (Exception e) {
            throw new RuntimeException("An error has occurred while reading the driver connection attribute " + name + " value for the connection (" + this + "). Error: " + e.getMessage(), e);
        }
        this.nativeDriverAttributes.put(name, nativeAttribute);
    }


    /**
     * @return the properties passed to the driver (native properties + tabli properties)
     * Object because value may be string, or boolean.
     * Example:
     * * {@link FileSystems#newFileSystem}
     * * {@link DriverManager#getConnection(String, Properties)}
     */
    public Map<String, Object> getConnectionProperties() {
        Map<String, Object> connectionProperties = this.getDefaultNativeDriverAttributes();
        Map<String, String> nativeProperties = this.getNativeDriverAttributes()
                .entrySet()
                .stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().getValueOrDefaultAsStringNotNull()
                ));
        connectionProperties.putAll(nativeProperties);
        return connectionProperties;
    }

    /**
     * @return the default native connection properties for the driver
     * They are recommended but the user can overwrite them.
     * Example: applicationName for jdbc, ...
     * The map returns object value because a boolean may be expected not only string
     */
    public Map<String, Object> getDefaultNativeDriverAttributes() {
        return new HashMap<>();
    }

    public SqlDataType<?> getSqlDataType(SqlDataTypeAnsi ansi) {

        return this.sqlDataTypeManager.getSqlDataType(ansi);

    }

    public SqlDataType<?> getSqlDataType(KeyNormalizer typeName) {
        return this.sqlDataTypeManager.getSqlDataType(typeName);
    }

    public SqlDataType<?> getSqlDataType(SqlDataTypeKeyInterface type) {

        Integer vendorTypeNumber = type.getVendorTypeNumber();
        if (vendorTypeNumber != 0) {
            return this.sqlDataTypeManager.getSqlDataType(type.toKeyNormalizer(), vendorTypeNumber);
        }
        return this.sqlDataTypeManager.getSqlDataType(type.toKeyNormalizer());

    }


    /**
     * A quick way to get a data path cast to the class
     * The type is determined by the path
     */
    public <T> T getDataPath(String path, Class<T> aClass) {
        DataPath dataPath = getDataPath(path);
        if (dataPath == null) {
            return null;
        }
        if (aClass.isAssignableFrom(dataPath.getClass())) {
            return aClass.cast(dataPath);
        }
        throw new InternalException("DataPath " + dataPath + " is not of type " + aClass + " but " + dataPath.getClass());
    }
}
