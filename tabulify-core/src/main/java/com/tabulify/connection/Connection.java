package com.tabulify.connection;

import com.tabulify.DbLoggers;
import com.tabulify.Tabular;
import com.tabulify.Vault;
import com.tabulify.conf.*;
import com.tabulify.fs.FsConnection;
import com.tabulify.model.SqlDataType;
import com.tabulify.model.SqlTypes;
import com.tabulify.noop.NoOpConnection;
import com.tabulify.spi.*;
import com.tabulify.uri.DataUri;
import com.tabulify.uri.DataUriString;
import net.bytle.exception.CastException;
import net.bytle.exception.InternalException;
import net.bytle.exception.NoValueException;
import net.bytle.type.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.sql.Ref;
import java.sql.Types;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

import static com.tabulify.conf.Origin.RUNTIME;

/**
 * A connection
 */
public abstract class Connection implements Comparable<Connection>, AutoCloseable {

  public static final String DATA_URI_SCHEME = "data-uri";

  // The connection name
  private final Tabular tabular;

  private final Random random = new Random();


  public Connection(Tabular tabular, com.tabulify.conf.Attribute name, com.tabulify.conf.Attribute uri) {

    this.tabular = tabular;

    try {
      // name check
      name.getValueOrDefault();
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
     * Creation of the SQL Data Type
     */
    addToStaticTypeMapping(SqlDataType.creationOf(this, Types.ARRAY).setSqlName("array").setSqlJavaClazz(java.sql.Array.class));
    addToStaticTypeMapping(SqlDataType.creationOf(this, Types.BIGINT).setSqlName("bigint").setSqlJavaClazz(BigInteger.class));
    addToStaticTypeMapping(SqlDataType.creationOf(this, Types.BINARY).setSqlName("binary").setSqlJavaClazz(byte[].class));
    addToStaticTypeMapping(SqlDataType.creationOf(this, Types.BIT).setSqlName("bit").setSqlJavaClazz(Boolean.class));
    addToStaticTypeMapping(SqlDataType.creationOf(this, Types.BLOB).setSqlName("blob").setSqlJavaClazz(java.sql.Blob.class));
    addToStaticTypeMapping(SqlDataType.creationOf(this, Types.BOOLEAN).setSqlName("boolean").setSqlJavaClazz(Boolean.class));
    addToStaticTypeMapping(SqlDataType.creationOf(this, Types.CHAR).setSqlName("char").setSqlJavaClazz(String.class).setDefaultPrecision(1));
    addToStaticTypeMapping(SqlDataType.creationOf(this, Types.CLOB).setSqlName("clob").setSqlJavaClazz(java.sql.Clob.class));
    addToStaticTypeMapping(SqlDataType.creationOf(this, Types.DATALINK).setSqlName("datalink").setSqlJavaClazz(java.net.URL.class));
    addToStaticTypeMapping(SqlDataType.creationOf(this, Types.DATE).setSqlName("date").setSqlJavaClazz(java.sql.Date.class));
    addToStaticTypeMapping(SqlDataType.creationOf(this, Types.DECIMAL).setSqlName("decimal").setSqlJavaClazz(java.math.BigDecimal.class));
    addToStaticTypeMapping(SqlDataType.creationOf(this, Types.DISTINCT).setSqlName("distinct"));
    addToStaticTypeMapping(SqlDataType.creationOf(this, Types.DOUBLE).setSqlName("double").setSqlJavaClazz(Double.class));
    addToStaticTypeMapping(SqlDataType.creationOf(this, Types.FLOAT).setSqlName("float").setSqlJavaClazz(Float.class));
    addToStaticTypeMapping(SqlDataType.creationOf(this, Types.INTEGER).setSqlName("integer").setSqlJavaClazz(Integer.class));
    addToStaticTypeMapping(SqlDataType.creationOf(this, SqlTypes.JSON).setSqlName("json").setSqlJavaClazz(String.class));
    addToStaticTypeMapping(SqlDataType.creationOf(this, Types.JAVA_OBJECT).setSqlName("java_object").setSqlJavaClazz(Object.class));
    addToStaticTypeMapping(SqlDataType.creationOf(this, Types.LONGNVARCHAR).setSqlName("longnvarchar").setSqlJavaClazz(String.class));
    addToStaticTypeMapping(SqlDataType.creationOf(this, Types.LONGVARBINARY).setSqlName("longvarbinary").setSqlJavaClazz(byte[].class));
    addToStaticTypeMapping(SqlDataType.creationOf(this, Types.LONGVARCHAR).setSqlName("longvarchar").setSqlJavaClazz(String.class));
    addToStaticTypeMapping(SqlDataType.creationOf(this, Types.NCHAR).setSqlName("nchar").setSqlJavaClazz(String.class).setDescription("setNString depending on the argument's size relative to the driver's limits on NVARCHAR"));
    addToStaticTypeMapping(SqlDataType.creationOf(this, Types.NCLOB).setSqlName("nclob").setSqlJavaClazz(java.sql.NClob.class));
    addToStaticTypeMapping(SqlDataType.creationOf(this, Types.NULL).setSqlName("null"));
    addToStaticTypeMapping(SqlDataType.creationOf(this, Types.NUMERIC).setSqlName("numeric").setSqlJavaClazz(java.math.BigDecimal.class));
    addToStaticTypeMapping(SqlDataType.creationOf(this, Types.NVARCHAR).setSqlName("nvarchar").setSqlJavaClazz(String.class));
    addToStaticTypeMapping(SqlDataType.creationOf(this, Types.OTHER).setSqlName("other"));
    addToStaticTypeMapping(SqlDataType.creationOf(this, Types.REAL).setSqlName("real").setSqlJavaClazz(Float.class));
    addToStaticTypeMapping(SqlDataType.creationOf(this, Types.REF).setSqlName("ref").setSqlJavaClazz(Ref.class));
    addToStaticTypeMapping(SqlDataType.creationOf(this, Types.ROWID).setSqlName("rowid").setSqlJavaClazz(java.sql.RowId.class));
    addToStaticTypeMapping(SqlDataType.creationOf(this, Types.SMALLINT).setSqlName("smallint").setSqlJavaClazz(Integer.class));
    addToStaticTypeMapping(SqlDataType.creationOf(this, Types.SQLXML).setSqlName("sqlxml").setSqlJavaClazz(java.sql.SQLXML.class));
    addToStaticTypeMapping(SqlDataType.creationOf(this, Types.STRUCT).setSqlName("struct").setSqlJavaClazz(java.sql.Struct.class));
    addToStaticTypeMapping(SqlDataType.creationOf(this, Types.TIME).setSqlName("time").setSqlJavaClazz(java.sql.Time.class));
    addToStaticTypeMapping(SqlDataType.creationOf(this, Types.TIME_WITH_TIMEZONE).setSqlName("time with time zone").setSqlJavaClazz(OffsetTime.class));
    addToStaticTypeMapping(SqlDataType.creationOf(this, Types.TIMESTAMP).setSqlName("timestamp").setSqlJavaClazz(java.sql.Timestamp.class)); // java.sql.Timestamp is based on UTC
    addToStaticTypeMapping(SqlDataType.creationOf(this, Types.TIMESTAMP_WITH_TIMEZONE).setSqlName("timestamp with time zone").setSqlJavaClazz(OffsetDateTime.class));
    addToStaticTypeMapping(SqlDataType.creationOf(this, Types.TINYINT).setSqlName("tinyint").setSqlJavaClazz(Integer.class));
    addToStaticTypeMapping(SqlDataType.creationOf(this, Types.VARBINARY).setSqlName("varbinary").setSqlJavaClazz(byte[].class));
    addToStaticTypeMapping(SqlDataType.creationOf(this, Types.VARCHAR).setSqlName("varchar").setSqlJavaClazz(String.class));

    /**
     * See page `Mapping from Java Type to Sql Type`
     * <a href="https://docs.oracle.com/javase/6/docs/technotes/guides/jdbc/getstart/mapping.html">mapping</a>
     * or
     * <a href="https://www.cis.upenn.edu/~bcpierce/courses/629/jdkdocs/guide/jdbc/getstart/mapping.doc.html">mapping</a>
     */
    sqlDataTypeByJavaClass.put(java.sql.Array.class, new Integer[]{Types.ARRAY});
    sqlDataTypeByJavaClass.put(java.sql.Blob.class, new Integer[]{Types.BLOB});
    sqlDataTypeByJavaClass.put(boolean.class, new Integer[]{Types.BOOLEAN}); // No Boolean
    sqlDataTypeByJavaClass.put(Boolean.class, new Integer[]{Types.BOOLEAN}); // No Boolean
    sqlDataTypeByJavaClass.put(byte.class, new Integer[]{Types.TINYINT});
    sqlDataTypeByJavaClass.put(Byte.class, new Integer[]{Types.TINYINT});
    sqlDataTypeByJavaClass.put(byte[].class, new Integer[]{Types.LONGVARBINARY}); // No Binary of varbinary
    sqlDataTypeByJavaClass.put(Byte[].class, new Integer[]{Types.LONGVARBINARY}); // No Binary of varbinary
    sqlDataTypeByJavaClass.put(BigDecimal.class, new Integer[]{Types.NUMERIC}); // Of Decimal ?
    sqlDataTypeByJavaClass.put(java.sql.Clob.class, new Integer[]{Types.CLOB});
    sqlDataTypeByJavaClass.put(java.sql.Date.class, new Integer[]{Types.DATE}); // And no Float
    sqlDataTypeByJavaClass.put(double.class, new Integer[]{Types.DOUBLE}); // And no Float
    sqlDataTypeByJavaClass.put(Double.class, new Integer[]{Types.DOUBLE}); // And no Float
    sqlDataTypeByJavaClass.put(float.class, new Integer[]{Types.REAL});
    sqlDataTypeByJavaClass.put(Float.class, new Integer[]{Types.REAL});
    sqlDataTypeByJavaClass.put(int.class, new Integer[]{Types.INTEGER}); // And not small int, tinyInt
    sqlDataTypeByJavaClass.put(Integer.class, new Integer[]{Types.INTEGER}); // And not small int, tinyInt
    sqlDataTypeByJavaClass.put(LocalDate.class, new Integer[]{Types.DATE});
    sqlDataTypeByJavaClass.put(LocalDateTime.class, new Integer[]{Types.TIMESTAMP}); // TIMESTAMP [ WITHOUT TIMEZONE ]
    sqlDataTypeByJavaClass.put(LocalTime.class, new Integer[]{Types.TIME}); // TIME [ WITHOUT TIMEZONE ]
    sqlDataTypeByJavaClass.put(long.class, new Integer[]{Types.BIGINT});
    sqlDataTypeByJavaClass.put(Long.class, new Integer[]{Types.BIGINT});
    sqlDataTypeByJavaClass.put(Object.class, new Integer[]{Types.JAVA_OBJECT});
    sqlDataTypeByJavaClass.put(OffsetTime.class, new Integer[]{Types.TIME_WITH_TIMEZONE}); // TIME [ WITH TIMEZONE ]
    sqlDataTypeByJavaClass.put(OffsetDateTime.class, new Integer[]{Types.TIMESTAMP_WITH_TIMEZONE}); // TIMESTAMP [ WITH TIMEZONE ]
    sqlDataTypeByJavaClass.put(java.sql.Ref.class, new Integer[]{Types.REF});
    sqlDataTypeByJavaClass.put(short.class, new Integer[]{Types.SMALLINT});
    sqlDataTypeByJavaClass.put(Short.class, new Integer[]{Types.SMALLINT});
    sqlDataTypeByJavaClass.put(java.sql.SQLXML.class, new Integer[]{Types.SQLXML});
    sqlDataTypeByJavaClass.put(java.sql.Struct.class, new Integer[]{Types.STRUCT});
    sqlDataTypeByJavaClass.put(java.sql.Time.class, new Integer[]{Types.TIME});
    sqlDataTypeByJavaClass.put(java.sql.Timestamp.class, new Integer[]{Types.TIMESTAMP});
    sqlDataTypeByJavaClass.put(String.class, new Integer[]{Types.VARCHAR, Types.CHAR, Types.LONGVARCHAR}); // No char of varchar, LongNVarchar, nchar, nvarchar
    sqlDataTypeByJavaClass.put(URL.class, new Integer[]{Types.DATALINK});


  }

  /**
   * Connection Variable. Variable managed by Tabli
   * Should be a known attribute
   */
  Map<AttributeEnumParameter, Attribute> attributes = new HashMap<>();
  /**
   * Driver Variable. Variable of the driver/library, not from us
   * We use attribute to add vault functionality
   * String is the original property
   */
  Map<String, Attribute> nativeDriverAttributes = new HashMap<>();

  /**
   * Which SQL type do we need to load the value of a java class
   */
  private final Map<Class<?>, Integer[]> sqlDataTypeByJavaClass = new HashMap<>();
  /**
   * Our Standard datatype by {@link Types type code}
   */
  private final Map<Integer, SqlDataType> sqlDataTypesByCode = new HashMap<>();


  /**
   * A utility tool to create the map
   *
   * @param sqlDataType the sql data type
   */
  private void addToStaticTypeMapping(SqlDataType sqlDataType) {
    sqlDataTypesByCode.put(sqlDataType.getTypeCode(), sqlDataType);
  }


  /**
   * Deep copy
   *
   * @param connection the origin connection
   * @return a new reference
   * Used in the {@link ConnectionVault#load(Path)}  datastore vault load function} to create a deep copy of the
   * internal data stores.
   */
  @SuppressWarnings("JavadocReference")
  public static Connection of(Connection connection) {
    return Connection.createConnectionFromProviderOrDefault(connection.getTabular(), connection.getNameAsAttribute(), connection.getUriAsVariable())
      .setAttributes(connection.getAttributes());

  }


  public Attribute getDescription() {
    return this.attributes.get(ConnectionAttributeEnumBase.DESCRIPTION);
  }


  public String getName() {

    try {
      return (String) getNameAsAttribute().getValueOrDefault();
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
    return this.getName();
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
      com.tabulify.conf.Attribute userAttribute = tabular.getVault().createAttribute(ConnectionAttributeEnumBase.USER, user, RUNTIME);
      this.addAttribute(userAttribute);
    } catch (Exception e) {
      throw new RuntimeException("Error while creating the user variable", e);
    }
    return this;
  }

  public Connection setPassword(String pwd) {
    try {
      com.tabulify.conf.Attribute password = tabular.getVault().createAttribute(ConnectionAttributeEnumBase.PASSWORD, pwd, RUNTIME);
      this.attributes.put(ConnectionAttributeEnumBase.PASSWORD, password);
    } catch (Exception e) {
      throw new RuntimeException("Error while creating the password variable for the connection (" + this + "). Error: " + e.getMessage(), e);
    }
    return this;
  }


  public com.tabulify.conf.Attribute getUser() {
    return this.attributes.get(ConnectionAttributeEnumBase.USER);
  }

  public com.tabulify.conf.Attribute getPasswordAttribute() {
    return this.attributes.get(ConnectionAttributeEnumBase.PASSWORD);
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

    return Uris.getScheme((String) getUriAsVariable().getValueOrDefaultOrNull());

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
      throw new RuntimeException("The connection attribute " + name + " is unknown for the connection " + this + ". We were expecting one of the following " + tabular.toPublicListOfParameters(this.getAttributeEnums()), e);
    }
    try {

      com.tabulify.conf.Attribute attribute = tabular.getVault().createAttribute(connectionAttributeBase, value, origin);
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
  List<Class<? extends AttributeEnumParameter>> getAttributeEnums() {
    return List.of(ConnectionAttributeEnumBase.class);
  }


  public Connection addAttribute(ConnectionAttributeEnum key, Object value) {
    try {
      com.tabulify.conf.Attribute attribute = tabular.getVault().createAttribute(key, value, RUNTIME);
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
    if (attributeEnum.equals(ConnectionAttributeEnumBase.NAME) && !attribute.getValueOrDefaultAsStringNotNull().equals(this.getName())) {
      throw new RuntimeException("You can't change the name of this connection from " + this.getName() + " to " + attribute.getValueOrDefaultAsStringNotNull());
    }
    if (attributeEnum.equals(ConnectionAttributeEnumBase.URI) && !attribute.getValueOrDefaultAsStringNotNull().equals(this.getUriAsString())) {
      throw new RuntimeException("You can't change the URI of this connection from " + this.getUriAsString() + " to  " + attribute.getValueOrDefaultAsStringNotNull());
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

  public static Connection createConnectionFromProviderOrDefault(Tabular tabular, String variableName, String variableUri) {

    try {
      return createConnectionFromProviderOrDefault(
        tabular,
        tabular.createAttribute(ConnectionAttributeEnumBase.NAME, variableName),
        tabular.createAttribute(ConnectionAttributeEnumBase.URI, variableUri)
      );
    } catch (Exception e) {
      throw new InternalException("Error while creating the main connection variable name/uri. Error: " + e.getMessage(), e);
    }
  }

  public static Connection createConnectionFromProviderOrDefault(Tabular tabular, com.tabulify.conf.Attribute attributeName, com.tabulify.conf.Attribute attributeUri) {

    /**
     * Name check
     * <p>
     * `/` are not supported because in ini file they may define a hierarchy and create then several datastore
     * http://ini4j.sourceforge.net/tutorial/IniTutorial.java.html
     */
    List<String> notAllowedCharacters = Arrays.asList(" ", "/");
    String nameString = (String) attributeName.getValueOrDefaultOrNull();
    notAllowedCharacters.forEach(c -> {
      if (nameString.contains(c)) {
        throw new RuntimeException("The datastore name (" + attributeName + ") contains the character (" + c + ") that is not allowed. To resolve this problem, you should delete it.");
      }
    });


    URI uri;
    String uriStringValue = (String) attributeUri.getValueOrDefaultOrNull();
    if (uriStringValue == null) {
      throw new RuntimeException("The uri of the connection (" + nameString + ") should not be null");
    }
    try {

      uri = UriEnhanced.createFromString(uriStringValue).toUri();

    } catch (Exception e) {
      String message = "The uri `" + uriStringValue + "` of the connection (" + attributeName + ") is not a valid.";
      if (uriStringValue.startsWith("\"") || uriStringValue.startsWith("'")) {
        message += " You should delete the character quote.";
      }
      message += " Error: " + e.getMessage();
      throw new RuntimeException(message, e);
    }

    if (uri.getScheme() != null && uri.getScheme().equals(DATA_URI_SCHEME)) {
      DataUriString dataUri = DataUriString.createFromString(uri.getSchemeSpecificPart());
      String connectionName = dataUri.getConnectionName();
      Connection connection = tabular.getConnection(connectionName);
      if (connection.getClass() != FsConnection.class) {
        throw new RuntimeException("The data-uri scheme is supported only with file system data uri. The connection " + connectionName + " is not a file system connection but a " + connection.getClass().getSimpleName() + " connection");
      }
      String resolve = connection.getCurrentDataPath().resolve(dataUri.getPath()).getAbsolutePath();
      attributeUri.setPlainValue("file://" + "/" + resolve.replace("\\", "/"));
    }

    List<ConnectionProvider> installedProviders = ConnectionProvider.installedProviders();
    for (ConnectionProvider connectionProvider : installedProviders) {
      if (connectionProvider.accept(attributeUri)) {
        return connectionProvider.createConnection(tabular, attributeName, attributeUri);
      }
    }

    // No provider was found
    final String message = "No provider was found from the connection (" + attributeName.getValueOrDefaultOrNull() + ") with the Uri (" + attributeUri.getValueOrDefaultOrNull() + ")";
    DbLoggers.LOGGER_DB_ENGINE.severe(message);
    return new NoOpConnection(tabular, attributeName, attributeUri);

  }


  public abstract DataSystem getDataSystem();


  /**
   * @param pathOrName - the pathOrName
   *                   * a {@link ResourcePath string path } if the second argument names is null,
   *                   * otherwise a {@link ResourcePath#getNames() name part} of the path string
   * @param mediaType  - the names
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
   * @return the current/working path of this data store
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


  public Connection setDescription(String description) {
    com.tabulify.conf.Attribute descVar;
    try {
      descVar = tabular.createAttribute(ConnectionAttributeEnumBase.DESCRIPTION, description);
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


  public abstract DataPath createScriptDataPath(DataPath dataPath);

  /**
   * @return all data types
   */
  public Set<SqlDataType> getSqlDataTypes() {

    return new HashSet<>(sqlDataTypesByCode.values());

  }

  /**
   * @param query - the query
   * @return a data path query
   */
  public DataPath createScriptDataPath(String query) {

    return createScriptDataPath(
      this.getTabular()
        .getAndCreateRandomMemoryDataPath()
        .setContent(query)
    );

  }

  /**
   * @param typeCode - the code type
   * @return the data type for one type
   */
  public SqlDataType getSqlDataType(Integer typeCode) {
    return sqlDataTypesByCode.get(typeCode);
  }

  /**
   * @param typeName - the type name
   * @return the data type from a sql type name
   */
  public SqlDataType getSqlDataType(String typeName) {
    return sqlDataTypesByCode
      .values()
      .stream()
      .filter(dt -> dt.getSqlName().equalsIgnoreCase(typeName))
      .findFirst()
      .orElse(null);
  }

  /**
   * @param clazz - the java class
   * @return @return the sql data type for a java class
   * This is not a bidirectional function with {@link SqlDataType#getClass()}
   * * The class of the SQL Data Type are the class needed by the driver to load the data type
   * * The class below is just a java class.
   */
  public SqlDataType getSqlDataType(Class<?> clazz) {

    Integer typeCode = Arrays.stream(sqlDataTypeByJavaClass.get(clazz))
      .findFirst()
      .orElse(null);
    return getSqlDataType(typeCode);

  }

  public abstract ProcessingEngine getProcessingEngine();

  /**
   * Sort of temporary data path
   *
   * @return a default typed data path with a UUID v4 name
   */
  public DataPath getAndCreateRandomDataPath(MediaType mediaType) {
    // We take only the first characters to only have alphabetic characters, no letter minus and
    // character that SQL or other system would not take
    String smallUid = UUID.randomUUID().toString().substring(0, 8);
    char randomChar = (char) ('a' + random.nextInt(26));
    // Hack, Sql name should start with a letter
    // It's a hack because name is for now immutable
    String name = randomChar + smallUid;
    return getDataPath(name, mediaType);
  }

  public DataPath getAndCreateRandomDataPath() {
    return getAndCreateRandomDataPath(null);
  }

  /**
   * Transform a value object into the desired clazz
   * <p>
   * For instance, this function in sql is used
   * in select stream to take over the {@link java.sql.ResultSet#getObject(String, Class)}
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
   * @return a string path object to manipulate the string version of the path of a {@link DataUri#getPath()}
   */
  public ResourcePath createStringPath(String pathOrName, String... names) {

    return new ConnectionResourcePathBase(pathOrName);

  }

  /**
   * Two data store may point to the same service
   * with two different current directory
   * <p>
   * This is the case with connection that:
   * * uses the local system
   * * but as two different local directory connection
   * <p>
   * When the service id is the same, the transfer will transfer the file
   * using remote command to the service and not using pull and push.
   *
   * @return the service id
   */
  public String getServiceId() {
    return (String) getUriAsVariable().getValueOrDefaultOrNull();
  }


  /**
   * Translate a sql data type from one connection to another
   *
   * @param sourceSqlDataType the source data type
   * @return the data type in the target connection
   */
  public SqlDataType getSqlDataTypeFromSourceDataType(SqlDataType sourceSqlDataType) {
    return this.getSqlDataType(sourceSqlDataType.getTypeCode());
  }


  /**
   * Try to create a connection
   *
   * @return true if the connection was pinged
   */
  public abstract Boolean ping();

  public Connection setOrigin(ConnectionOrigin connectionOrigin) {
    this.getAttribute(ConnectionAttributeEnumBase.ORIGIN)
      .setPlainValue(connectionOrigin);
    return this;
  }

  public ConnectionOrigin getOrigin() {
    try {
      return (ConnectionOrigin) this.getAttribute(ConnectionAttributeEnumBase.ORIGIN).getValueOrDefault();
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
  public static String getConnectionNameFromUri(URI uri) {
    String host = uri.getHost();
    String name = "";
    if (host != null) {
      name = host;
    }
    return (name + uri.getPath()).replace("/", "\\");
  }


  public com.tabulify.conf.Attribute getAttribute(ConnectionAttributeEnum attribute) {
    com.tabulify.conf.Attribute variable = this.attributes.get(attribute);
    if (variable == null) {
      /**
       * connection attribute should be present
       * added via {@link #addAttributesFromEnumAttributeClass(Class)} in the constructor
       */
      throw new RuntimeException("The connection attribute " + attribute + " was not found. Did you add it at construction time");
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
    for (ConnectionAttributeEnum attribute : enumClass.getEnumConstants()) {

      Vault vault = this.tabular.getVault();
      Vault.VariableBuilder variableBuilder = vault.createVariableBuilderFromAttribute(attribute);

      // What is fucked up, is fucked up
      // Name and uri are constructor variable and are therefore already added
      if (attribute == ConnectionAttributeEnumBase.NAME || attribute == ConnectionAttributeEnumBase.URI) {
        continue;
      }

      // Env
      // We don't look up without the tabli predix because it can cause clashes
      // for instance, name in os is the name of the computer
      TabularEnvs tabularEnvs = this.tabular.getTabularEnvs();
      KeyNormalizer envName = tabularEnvs.getOsTabliEnvName(attribute);
      String envValue = tabularEnvs.getOsEnvValue(envName);
      if (envValue != null) {
        com.tabulify.conf.Attribute variable = variableBuilder
          .setOrigin(com.tabulify.conf.Origin.OS)
          .buildSafe(envValue);
        this.addAttribute(variable);
        continue;
      }

      // None
      com.tabulify.conf.Attribute variable = variableBuilder
        .setOrigin(Origin.RUNTIME)
        .buildSafe(null);
      this.addAttribute(variable);

    }
    return this;
  }

  public String getUriAsString() {

    try {
      return (String) getUriAsVariable().getValueOrDefault();
    } catch (NoValueException e) {
      throw new InternalException("Uri was not found in the connection (" + this + ")");
    }

  }


  public Map<String, String> getNativeDriverAttributes() {
    return this.nativeDriverAttributes
      .entrySet()
      .stream()
      .collect(Collectors.toMap(
        Map.Entry::getKey,
        e -> e.getValue().getValueOrDefaultAsStringNotNull()
      ));
  }


  public void putNativeAttribute(String name, Attribute attribute) {
    this.nativeDriverAttributes.put(name, attribute);
  }

}
