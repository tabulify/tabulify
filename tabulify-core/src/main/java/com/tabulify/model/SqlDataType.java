package com.tabulify.model;

import com.tabulify.connection.Connection;
import net.bytle.exception.InternalException;
import net.bytle.exception.MissingSwitchBranch;
import net.bytle.type.KeyInterface;
import net.bytle.type.KeyNormalizer;

import java.sql.DatabaseMetaData;
import java.sql.JDBCType;
import java.sql.Types;
import java.util.*;
import java.util.stream.Collectors;

/**
 * The datatype of a column.
 * It's a wrapper over the {@link DatabaseMetaData#getTypeInfo()} data type of database that allows
 * * to manage alias and synonym (ie tree relationship, a data type may have a parent and children)
 * * to add {@link #getValueClass()} java type class
 * <p>
 * Types may be aliased, we have a tree of 1 level
 * For instance,
 * * {@link Types#DECIMAL} in Postgres is the same as {@link Types#NUMERIC}
 * * or {@link Types#FLOAT} in Postgres is the same as {@link Types#REAL}
 * * or {@link Types#NVARCHAR} in Postgres is the same as {@link Types#VARCHAR} because string are stored in Unicode
 * * or {@link Types#NCHAR} in Postgres is the same as {@link Types#CHAR} because string are stored in Unicode
 * * or `float8` in Postgres is an alias of {@link Types#DOUBLE}
 * * or `time` is an alias of `time without time zone`
 * * ...
 * A child will inherit the specifier (ie {@link #getMaxPrecision() precision/length}, {@link #getMaximumScale() max scale}, {@link #getMinimumScale() min scale}
 * Note that a child may have another {@link Types} that its parent
 * For instance, SQLLite affinity. {@link Types#INTEGER} covers all int type such as {@link Types#SMALLINT}, {@link Types#TINYINT}, {@link Types#BIGINT}
 * <p>
 * See also:
 * * <a href="https://docs.oracle.com/javase/8/docs/api/java/sql/DatabaseMetaData.html#getTypeInfo--">Jdbc Meta Info</a>
 * * <a href="https://developers.google.com/public-data/docs/schema/dspl18">...</a>
 * * <a href="https://html.spec.whatwg.org/#attr-input-typ">...</a> - Html Forms Attributes Type
 * * <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/mapping-types.html">...</a> - Elastic Search
 * * <a href="https://tools.ietf.org/html/draft-zyp-json-schema-03#section-5.1">...</a> - Json schema
 * * <a href="https://www.w3.org/TR/2015/REC-tabular-metadata-20151217/#metadata-format">...</a> - See data type description
 */
public class SqlDataType<T> implements Comparable<SqlDataType<T>>, SqlDataTypeVendor {


  private final SqlDataTypeBuilder<T> builder;


  public SqlDataType(SqlDataTypeBuilder<T> sqlDataTypeBuilder) {
    this.builder = sqlDataTypeBuilder;

  }


  /**
   * Use the {@link #builder(Connection, KeyNormalizer, SqlDataTypeAnsi)}  instead
   */
  public static SqlDataTypeBuilder<?> creationOf(Connection connection, int typeCode, String typeName) {
    KeyNormalizer safe = KeyNormalizer.createSafe(typeName);
    SqlDataTypeAnsi standard = SqlDataTypeAnsi.cast(safe, typeCode);
    return SqlDataType.builder(connection, safe, standard);
  }


  /**
   * The java class to get this object
   * {@link java.sql.ResultSet#getObject(int, Class)}
   */
  @Override
  public Class<T> getValueClass() {

    return this.builder.javaClazz;

  }


  /**
   * The maximum precision that the server supports for the given datatype for this type or 0 if there is no precision.
   *
   * <p>
   * This is the <a href="https://docs.oracle.com/javase/8/docs/api/java/sql/DatabaseMetaData.html#getTypeInfo--">PRECISION</a>  column value
   * Precision is also known as <a href="https://docs.oracle.com/javase/8/docs/api/java/sql/DatabaseMetaData.html#getColumns-java.lang.String-java.lang.String-java.lang.String-java.lang.String-">COLUMN_SIZE</a>
   * * For numeric data, this is the maximum precision.
   * * For character data, this is the maximum length in characters.
   * * For datetime datatypes, this is the maximum length in characters of the String representation (assuming the maximum allowed precision of the fractional seconds component).
   * * For binary data, this is the maximum length in bytes.
   * * For the ROWID datatype, this is the maximum length in bytes.
   * * For integer, the number of digits of the maximum value (Example for an int4, the maximum value (2,147,483,647) has 10 digits)
   */
  @Override
  public int getMaxPrecision() {
    if (this.builder.parent != null) {
      return this.builder.parent.getMaxPrecision();
    }
    /**
     * Special case for Integer
     * So that the precision is the same
     * for all vendor
     * Precision is only an information for integer
     */
    if (this.builder.maxPrecision == 0 && !this.getUnsignedAttribute()) {
      switch (getAnsiType()) {
        case INTEGER:
          return SqlDataTypeManager.INTEGER_SIGNED_MAX_LENGTH;
        case SMALLINT:
          return SqlDataTypeManager.SMALLINT_SIGNED_MAX_LENGTH;
        case TINYINT:
          return SqlDataTypeManager.TINYINT_SIGNED_MAX_LENGTH;
        case MEDIUMINT:
          return SqlDataTypeManager.MEDIUMINT_SIGNED_MAX_LENGTH;
        case BIGINT:
          return SqlDataTypeManager.BIGINT_SIGNED_MAX_LENGTH;
      }
    }
    return this.builder.maxPrecision;
  }

  /**
   * @return prefix used to quote a literal value (maybe null)
   * Source: <a href="https://docs.oracle.com/javase/8/docs/api/java/sql/DatabaseMetaData.html#getTypeInfo--">...</a>
   */
  public String getLiteralPrefix() {
    if (this.builder.parent != null) {
      return this.builder.parent.getLiteralPrefix();
    }
    return this.builder.literalPrefix;
  }

  /**
   * @return suffix used to quote a literal value (maybe null)
   * Source: <a href="https://docs.oracle.com/javase/8/docs/api/java/sql/DatabaseMetaData.html#getTypeInfo--">...</a>
   */
  public String getLiteralSuffix() {
    if (this.builder.parent != null) {
      return this.builder.parent.getLiteralSuffix();
    }
    return this.builder.literalSuffix;
  }

  /**
   * @return parameters used in creating the type (maybe null)
   */
  public String getCreateParams() {
    if (this.builder.parent != null) {
      return this.builder.parent.getCreateParams();
    }
    return this.builder.createParams;
  }

  /**
   * @return can you use null for this type
   * * {@link DatabaseMetaData#typeNoNulls} - does not allow NULL values
   * * {@link DatabaseMetaData#typeNullable} - allows NULL values
   * * {{@link DatabaseMetaData#typeNullableUnknown} - nullability unknown
   */
  public SqlDataTypeNullable getNullable() {
    if (this.builder.parent != null) {
      return this.builder.parent.getNullable();
    }
    return this.builder.nullable;
  }

  /**
   * @return is it case-sensitive
   */
  public Boolean getCaseSensitive() {
    return this.builder.caseSensitive;
  }

  /**
   * @return can you use "WHERE" based on this type:
   * * typePredNone - No support
   * * typePredChar - Only supported with WHERE .. LIKE
   * * typePredBasic - Supported except for WHERE .. LIKE
   * * typeSearchable - Supported for all WHERE ..
   */
  public Short getSearchable() {
    if (this.builder.parent != null) {
      return this.builder.parent.getSearchable();
    }
    return this.builder.searchable;
  }

  /**
   * @return is it unsigned
   * Not null, the value is null only with the function {@link #getAttributeValue(SqlDataTypeAttribute)} if the type is not a number
   */
  public Boolean getUnsignedAttribute() {
    if (this.builder.parent != null) {
      return this.builder.parent.getUnsignedAttribute();
    }
    return this.builder.unsignedAttribute;
  }

  /**
   * @return return if it can a money value.
   */
  public Boolean getIsFixedPrecisionScale() {
    if (this.builder.parent != null) {
      return this.builder.parent.getIsFixedPrecisionScale();
    }
    return this.builder.isFixedPrecisionScale;
  }

  /**
   * @return if we can add an auto-increment property to this type (can it be used for an auto-increment value)
   * Not null, the value is null only with the function {@link #getAttributeValue(SqlDataTypeAttribute)} if the type is not a number
   */
  public Boolean getAutoIncrement() {
    if (this.builder.parent != null) {
      return this.builder.parent.getAutoIncrement();
    }
    return this.builder.autoIncrement;
  }

  /**
   * @return localized version of type name (maybe null) - in English, French ...
   */
  public String getLocalTypeName() {

    if (this.builder.parent != null) {
      return this.builder.parent.getLocalTypeName();
    }
    return this.builder.localTypeName;
  }

  /**
   * @return minimum scale supported
   * See {@link DatabaseMetaData#getTypeInfo()}
   */
  public int getMinimumScale() {
    if (this.builder.parent != null) {
      return this.builder.parent.getMinimumScale();
    }
    return this.builder.minimumScale;
  }

  /**
   * @return maximum scale supported
   */
  @Override
  public int getMaximumScale() {
    if (this.builder.parent != null) {
      return this.builder.parent.getMaximumScale();
    }
    return this.builder.maximumScale;
  }


  @Override
  public String toString() {
    return toKeyNormalizer() + " (" + getVendorTypeNumber() + "," + getAnsiType() + ")@" + this.builder.connection.getName();
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    SqlDataType<?> that = (SqlDataType<?>) o;
    return this.builder.sqlTypeKey.equals(that.builder.sqlTypeKey);
  }

  @Override
  public int hashCode() {
    return this.builder.sqlTypeKey.hashCode();
  }

  /**
   * The type name in a {@link KeyNormalizer} form
   **/
  public KeyNormalizer toKeyNormalizer() {
    return this.builder.sqlTypeKey.toKeyNormalizer();
  }

  /**
   * The type name used in SQL statement
   **/
  @Override
  public String getName() {
    return this.builder.sqlTypeKey.toKeyNormalizer().toSqlTypeCase();
  }

  @Override
  public String getVendor() {
    return this.builder.connection.toString();
  }

  /**
   * @return the type code given by the driver
   */
  @Override
  public Integer getVendorTypeNumber() {
    return this.builder.sqlTypeKey.getVendorTypeNumber();
  }


  public boolean isNumber() {
    return Number.class.isAssignableFrom(this.getValueClass())
      || SqlDataTypes.numberTypes.contains(this.getAnsiType());
  }

  /**
   * The standard spec to which this data type is attached
   * <p>
   * Note that this is not the same {@link #toKeyNormalizer()}
   * which the name used in the SQL statement
   * Note that if you want the final ANSI type for a column,
   * you should use {@link ColumnDef#getAnsiType()}
   * this function does the conversion bit(1) to Boolean
   */
  @Override
  public SqlDataTypeAnsi getAnsiType() {
    if (this.getParent() != null) {
      return this.getParent().getAnsiType();
    }
    return this.builder.sqlDataTypeAnsi;
  }


  /**
   * @return the default precision if set or o if unknown
   * Previously, the default precision was the max precision but SQL Server
   * has a `max` specifier to define a clob. Therefore, we need to return 0 if not set
   * so that SQLServer can use `max` if not set instead of the maximum precision numeric value
   */
  public int getDefaultPrecision() {
    if (this.builder.parent != null) {
      return this.builder.parent.getDefaultPrecision();
    }
    return this.builder.defaultPrecision;
  }


  /**
   * Do we need to add the specifier (n,p,s) in a sql statement if known is specified
   */
  public Boolean getIsSpecifierMandatory() {
    if (this.builder.parent != null) {
      return this.builder.parent.getIsSpecifierMandatory();
    }
    return this.builder.mandatorySpecifier;
  }


  @Override
  public int compareTo(SqlDataType o) {

    return this.toKeyNormalizer().compareTo(o.toKeyNormalizer());

  }


  public static SqlDataTypeBuilder<?> builder(Connection connection, KeyNormalizer typeName, SqlDataTypeAnsi ansiType) {
    return builder(connection, typeName, ansiType.getVendorTypeNumber())
      .setAnsiType(ansiType);
  }

  public static SqlDataTypeBuilder<?> builder(Connection connection, String typeName, SqlDataTypeAnsi ansiType) {
    return builder(connection, KeyNormalizer.createSafe(typeName), ansiType.getVendorTypeNumber())
      .setAnsiType(ansiType);
  }

  public static SqlDataTypeBuilder<?> builder(Connection connection, KeyNormalizer typeName, int typeCode) {
    SqlDataTypeKey sqlDataTypeKey = new SqlDataTypeKey(connection, typeName, typeCode);
    SqlDataTypeAnsi ansiType = SqlDataTypeAnsi.cast(typeName, typeCode);
    Class<?> clazz = ansiType.getValueClass();
    return new SqlDataTypeBuilder<>(connection, sqlDataTypeKey, clazz)
      .setAnsiType(ansiType);
  }

  public static <T> SqlDataTypeBuilder<T> builder(Connection connection, SqlDataTypeKey sqlDataTypeKey, Class<T> aClass) {
    return new SqlDataTypeBuilder<>(connection, sqlDataTypeKey, aClass);
  }

  public static SqlDataTypeBuilder<?> builder(Connection connection, KeyInterface typeName, SqlDataTypeAnsi ansiType) {
    return builder(connection, typeName.toKeyNormalizer(), ansiType)
      .setAnsiType(ansiType);
  }

  public static SqlDataTypeBuilder<?> builder(Connection connection, SqlDataTypeAnsi sqlDataTypeAnsi) {
    return builder(connection, sqlDataTypeAnsi.toKeyNormalizer(), sqlDataTypeAnsi)
      .setAnsiType(sqlDataTypeAnsi);
  }

  public static SqlDataTypeBuilder<?> builder(Connection connection, SqlDataTypeKeyInterface sqlType) {
    return builder(connection, sqlType.toKeyNormalizer(), sqlType.getVendorTypeNumber());
  }


  public SqlDataTypePriority getPriority() {
    /**
     * The priority does not depend on the alias
     * As the alias may get a higher priority
     * For instance, Postgres returns as integer, int4,
     * we alis therefore the integer name to in4,
     * but we want to use integer as first name
     */
    if (this.builder.priority != null) {
      return this.builder.priority;
    }
    /**
     * The default
     */
    return this.builder.sqlDataTypeAnsi.getPriority();
  }


  @Override
  public String getDescription() {
    if (this.builder.description != null) {
      return this.builder.description;
    }
    return this.builder.sqlDataTypeAnsi.getDescription();
  }

  @Override
  public List<KeyInterface> getAliases() {
    return new ArrayList<>(this.builder.childrenType);
  }

  public Connection getConnection() {
    return this.builder.connection;
  }

  /**
   * @return if type has a precision (ie {@link #getMaxPrecision()} is not zero
   */
  public boolean hasPrecision() {
    return this.getMaxPrecision() != 0;
  }

  /**
   * @return if type has a precision (ie {@link #getMaximumScale()} is not zero
   */
  public boolean hasScale() {
    return this.getMaximumScale() != 0;
  }


  /**
   * @return The parent or null if this is the top type
   * An alias has only one parent.
   */
  public SqlDataType<T> getParent() {
    return this.builder.parent;
  }

  public Set<SqlDataType<T>> getChildrenAlias() {
    return this.builder.childrenType;
  }

  public SqlDataType<T> getParentOrSelf() {
    SqlDataType<T> parent = this.getParent();
    if (parent != null) {
      return parent;
    }
    return this;
  }

  public SqlDataType<T> addChild(SqlDataType<T> typeBuilder) {
    if (typeBuilder.toKeyNormalizer().equals(this.toKeyNormalizer())) {
      throw new InternalException("A name can not be a child of itself. Name: " + this.toKeyNormalizer());
    }
    if (typeBuilder.toKeyNormalizer().equals(this.toKeyNormalizer())) {
      throw new InternalException("A name can not be a child of itself. Name: " + this.toKeyNormalizer());
    }
    this.builder.childrenType.add(typeBuilder);
    return this;
  }

  public boolean getIsSupported() {
    return this.builder.sqlDataTypeAnsi.getIsSupported();
  }

  public boolean isInteger() {
    // Bigint is Long, not integer so we test
    return Integer.class.isAssignableFrom(this.getValueClass())
      || SqlDataTypes.numberIntegerTypes.contains(this.getAnsiType());
  }

  public SqlDataTypeKey getKey() {
    return builder.getTypeKey();
  }

  /**
   * @return the external/public value for printing
   */
  public Object getAttributeValue(SqlDataTypeAttribute sqlDataTypeAttribute) {
    switch (sqlDataTypeAttribute) {
      case NAME:
        return toKeyNormalizer().toSqlTypeCase();
      case ALIASES:
        return getChildrenAlias().stream().map(SqlDataType::toKeyNormalizer).sorted().map(KeyNormalizer::toSqlTypeCase).collect(Collectors.joining(", "));
      case ANSI_TYPE:
        return getAnsiType() == SqlDataTypeAnsi.OTHER ? "" : getAnsiType().toKeyNormalizer().toSqlTypeCase();
      case MAX_PRECISION:
        int maxPrecision = getMaxPrecision();
        return maxPrecision == 0 ? null : maxPrecision;
      case SUPPORTED:
        return getIsSupported();
      case MIN_SCALE:
        int minimumScale = getMinimumScale();
        return minimumScale == 0 ? null : minimumScale;
      case MAX_SCALE:
        int maximumScale = getMaximumScale();
        return maximumScale == 0 ? null : maximumScale;
      case CLASS:
        return getValueClass();
      case DESCRIPTION:
        return getDescription();
      case JDBC_CODE:
        return getVendorTypeNumber();
      case JDBC_NAME:
        try {
          JDBCType jdbcType = JDBCType.valueOf(getVendorTypeNumber());
          return jdbcType.getName();
        } catch (Exception e) {
          return "";
        }
      case AUTO_INCREMENT:
        if (!this.isNumber()) {
          return null;
        }
        return this.getAutoIncrement();
      case UNSIGNED:
        // All text are unsigned, and it's messing up the printing data
        if (!this.isNumber()) {
          return null;
        }
        return this.getUnsignedAttribute();
      case FIXED_PRECISION_SCALE:
        // All text are false, and it's messing up the printing data
        if (!this.isNumber()) {
          return null;
        }
        return this.getIsFixedPrecisionScale();
      case PARAMETERS:
        return this.getCreateParams();
      default:
        throw new MissingSwitchBranch("sqlDataTypeAttribute", sqlDataTypeAttribute);
    }
  }

  @Override
  public String name() {
    return getName();
  }

  /**
   * Overwrite the ANSI type
   * This is to handle the regular case when a bit(1) is a boolean not a bit
   */
  public SqlDataType<T> setAnsiType(SqlDataTypeAnsi sqlDataTypeAnsi) {
    this.builder.sqlDataTypeAnsi = sqlDataTypeAnsi;
    return this;
  }

  /**
   * Not generic because the class can change
   * For instance, Postgres returns a {@link Types#TIMESTAMP} type code
   * for a timestamptz, but in java, we need to use Offset
   */
  public static class SqlDataTypeBuilder<T> {


    /**
     * Connection is important on type translation
     * to determine if it should be translated
     * See {@link Connection#getSqlDataTypeFromSourceColumn}
     */
    private final Connection connection;

    /**
     * The unique identifier
     */
    private final SqlDataTypeKey sqlTypeKey;
    /**
     * The manager for context
     * (Maybe null if the type is created for testing purpose)
     */
    private SqlDataTypeManager manager;


    /**
     * The type of data stored in this type
     * <p>
     * Never NULL, at least the value {@link SqlDataTypeAnsi#OTHER}
     */
    private SqlDataTypeAnsi sqlDataTypeAnsi;


    /**
     * Not sure what this is as name.
     * Name could be translated in the local language ?
     */
    private String localTypeName; // localized version of type name (maybe null)

    /**
     * The java class expected of the object
     * ie {@link java.sql.ResultSet#getObject(int, Class)}
     * the counterpart of {@link java.sql.PreparedStatement#setObject(int, Object, int)}
     */
    Class<T> javaClazz;

    /**
     * Others properties
     */
    // maximum precision (0=no precision)
    // 0 follows the jdbc reference that states that if the value is SQL `NULL`, the value returned is 0
    // this is stated when retrieving the type, but also in the resultSet with getInt (it does not return any Integer but int)
    private int maxPrecision = 0;
    private String literalPrefix; // prefix used to quote a literal (maybe null)
    private String literalSuffix; // suffix used to quote a literal (maybe null)
    /**
     * Parameters used in creating the type (maybe null)
     * This is a template thing
     * Example for a char in mySQL:
     * [(M)] [CHARACTER SET charset_name] [COLLATE collation_name]
     */
    private String createParams;
    /**
     * One value of
     * {@link DatabaseMetaData#typeNoNulls}
     * {@link DatabaseMetaData#typeNullable}
     * {@link DatabaseMetaData#typeNullableUnknown}
     * int because it can't be null, if this is the case use unknown
     */
    private SqlDataTypeNullable nullable; // can you use null for this type
    private Boolean caseSensitive; // is it case-sensitive
    private Short searchable; // can you use "WHERE" based on this type:
    /**
     * ie for Tinyint for instance
     * if signed (false, default), you can use the value -128 to 127
     * if unsigned (true), you can use the value 0 to 255
     * <a href="https://dev.mysql.com/doc/refman/8.4/en/integer-types.html">...</a>
     */
    private Boolean unsignedAttribute = false;
    /**
     * Definition in <a href="https://docs.oracle.com/javase/8/docs/api/java/sql/DatabaseMetaData.html#getTypeInfo--">...</a>
     * `can it be a money value`
     * The column name FIXED_PREC_SCALE is somewhat misleading - it's really asking "is this data type appropriate for monetary values?" rather than describing whether precision and scale are fixed.
     * Other definition, does the number has a fixed precision or scale
     * The data type always has a consistent and unchangeable precision and scale
     * (Integer is fixed)
     * Money requires fixed precision and scale - Financial calculations need exact decimal arithmetic, not floating-point approximations that can introduce rounding errors
     * SQL Server follows the money rule
     * <a href="https://learn.microsoft.com/en-us/sql/connect/jdbc/reference/gettypeinfo-method-sqlserverdatabasemetadata?view=sql-server-ver17">SQL Server</a>
     */
    private Boolean isFixedPrecisionScale;
    /**
     * Def: Can the type be used for an auto-increment value
     * <a href="https://docs.oracle.com/javase/8/docs/api/java/sql/DatabaseMetaData.html#getTypeInfo--">Ref</a>
     * Depending on the database, the value may be true when:
     * * the type is an autoincrement, and you don't add autoincrement property
     * * the type is not an internal autoincrement, but you can add it
     * <p>
     * Example:
     * * Internal autoincrement increment added in the name
     *   * <a href="https://www.postgresql.org/docs/current/datatype-numeric.html">Postgres</a> All serial type are autoincrement (`smallserial`, ...) but the other (smallint) are not
     *   * In SQL Server, the type `int identity` (returned by the driver)
     * <p>
     * * Auto Increment added as property
     *   * in MySQL, the auto increment key (ie `id MEDIUMINT NOT NULL AUTO_INCREMENT`)
     */
    private Boolean autoIncrement = false;
    // minimum scale supported
    // 0 follows the jdbc reference that states that if the value is SQL `NULL`, the value returned is 0
    // scale could be a short as the returned value because jdbc use it but,
    // * you can't write a short literal (ie 0 is an integer)
    // * converting it is a pain
    // We keep with then a int
    private int minimumScale = 0;
    // maximum scale supported
    // 0 follows the jdbc reference that states that if the value is SQL `NULL`, the value returned is 0
    private int maximumScale = 0;
    @SuppressWarnings({"FieldCanBeLocal", "unused"})
    private String description;
    // The default precision if not specified
    // 0 == null
    // 0 follows the jdbc reference that states that if the value is SQL `NULL`, the value returned is 0
    private int defaultPrecision = 0;
    // if the precision/length specifier is mandatory in a clause statement
    private Boolean mandatorySpecifier;
    private SqlDataTypePriority priority = SqlDataTypePriority.DEFAULT; // if there is multiple sql type selected for the same java class/types, the highest priority win

    /**
     * The parent is the public name of the type
     * For instance, in postgres, the driver returns for integer `int4`
     * but the information_schema.columns returns `integer`
     * The parent is then `integer` and is used in all printing for now
     * (statement, column definition,...)
     */
    private SqlDataType<T> parent = null;
    private SqlDataTypeBuilder<T> parentBuilder = null;


    private final Set<SqlDataType<T>> childrenType = new HashSet<>();


    /**
     * We created a constructor with this 3 mandatory elements because
     * we got tired of chasing where in the code, a type code was not set
     */
    public SqlDataTypeBuilder(Connection connection, SqlDataTypeKey typeKey, Class<T> aClass) {

      Objects.requireNonNull(typeKey, "type key must not be null");
      // Java class
      Objects.requireNonNull(aClass, "Java class cannot be null. It's null for the type: " + typeKey);
      this.sqlTypeKey = typeKey;
      this.javaClazz = aClass;
      this.connection = connection;

    }

    /**
     * The manager is not mandatory (in test we create type without any manager)
     * This function is typically called by the manager to inject itself
     * so that we can use {@link #addChildAliasName(String)}
     */
    public SqlDataTypeBuilder<T> setManager(SqlDataTypeManager manager) {
      this.manager = manager;
      return this;
    }


    /**
     * @param maxPrecision - the maximum precision
     */
    public SqlDataTypeBuilder<T> setMaxPrecision(int maxPrecision) {
      if (maxPrecision < 0) {
        // oracle returns -1 for blob, clob
        // 0 means also no precision
        this.maxPrecision = 0;
        return this;
      }
      this.maxPrecision = maxPrecision;
      return this;
    }

    public SqlDataTypeBuilder<T> setLiteralPrefix(String literalPrefix) {
      this.literalPrefix = literalPrefix;
      return this;
    }

    public SqlDataTypeBuilder<T> setLiteralSuffix(String literalSuffix) {
      this.literalSuffix = literalSuffix;
      return this;
    }

    public SqlDataTypeBuilder<T> setCreateParams(String createParams) {
      this.createParams = createParams;
      return this;
    }

    public SqlDataTypeBuilder<T> setNullable(SqlDataTypeNullable nullable) {
      this.nullable = nullable;
      return this;
    }

    public SqlDataTypeBuilder<T> setCaseSensitive(Boolean caseSensitive) {
      this.caseSensitive = caseSensitive;
      return this;
    }

    public SqlDataTypeBuilder<T> setSearchable(Short searchable) {
      this.searchable = searchable;
      return this;
    }


    public SqlDataTypeBuilder<T> setUnsignedAttribute(Boolean unsignedAttribute) {
      this.unsignedAttribute = unsignedAttribute;
      return this;
    }

    /**
     * The precision that will be created when not given
     */
    public SqlDataTypeBuilder<T> setDefaultPrecision(int defaultPrecision) {
      this.defaultPrecision = defaultPrecision;
      return this;
    }


    /**
     * If the precision should be in the statement
     * Example nvarchar2 for oracle, if true, the specifier (p,s,n) is mandatory
     */
    public SqlDataTypeBuilder<T> setMandatorySpecifier(Boolean mandatorySpecifier) {
      this.mandatorySpecifier = mandatorySpecifier;
      return this;
    }

    public SqlDataTypeBuilder<T> setIsFixedPrecisionScale(Boolean isFixedPrecisionScale) {
      this.isFixedPrecisionScale = isFixedPrecisionScale;
      return this;
    }

    /**
     * @param autoIncrement - true if this type is an auto-incrementing type
     *                      For instance, for postgres, <a href="https://www.postgresql.org/docs/current/datatype-numeric.html">...</a>
     *                      `smallint` is not autoincrement but `smallserial` is
     */
    public SqlDataTypeBuilder<T> setAutoIncrement(Boolean autoIncrement) {
      this.autoIncrement = autoIncrement;
      return this;
    }

    public SqlDataTypeBuilder<T> setLocalTypeName(String localTypeName) {
      this.localTypeName = localTypeName;
      return this;
    }

    public SqlDataTypeBuilder<T> setMinimumScale(int minimumScale) {
      this.minimumScale = minimumScale;
      return this;
    }

    public SqlDataTypeBuilder<T> setMaximumScale(int maximumScale) {
      this.maximumScale = maximumScale;
      return this;
    }


    public SqlDataTypeBuilder<T> setDescription(String description) {
      this.description = description;
      return this;
    }

    public SqlDataType<T> build() {


      /**
       * The type code gives the format of the type
       * (such as do we need a precision and a scale?)
       * It's mandatory
       */
      if (sqlDataTypeAnsi == null) {
        sqlDataTypeAnsi = SqlDataTypeAnsi.cast(this.sqlTypeKey.toKeyNormalizer(), this.sqlTypeKey.getVendorTypeNumber());
      }


      return new SqlDataType<>(this);

    }


    /**
     * @param priority - a priority
     *                 Set a priority greater than {@link SqlDataTypePriority} if you want to win for sure
     */
    public SqlDataTypeBuilder<T> setPriority(SqlDataTypePriority priority) {
      this.priority = priority;
      return this;
    }


    public Integer getTypeCode() {
      return this.sqlTypeKey.getVendorTypeNumber();
    }


    public KeyNormalizer getName() {
      return this.sqlTypeKey.toKeyNormalizer();
    }

    public void setParent(SqlDataType<T> parent) {
      this.parent = parent;
    }

    @Override
    public String toString() {
      return sqlTypeKey.toString();
    }


    public SqlDataTypeBuilder<T> setParentBuilder(SqlDataTypeBuilder<T> parentBuilder) {
      if (parentBuilder.getName().equals(this.getName())) {
        throw new InternalError("The parent cannot have the same name for the type `" + this + "`");
      }
      this.parentBuilder = parentBuilder;
      return this;
    }

    public SqlDataTypeBuilder<T> getParentBuilder() {
      return this.parentBuilder;
    }

    public SqlDataTypeBuilder<T> setAnsiType(SqlDataTypeAnsi typedName) {
      this.sqlDataTypeAnsi = typedName;
      return this;
    }

    @Override
    public boolean equals(Object o) {
      if (o == null || getClass() != o.getClass()) return false;
      SqlDataTypeBuilder<?> that = (SqlDataTypeBuilder<?>) o;
      return sqlTypeKey.equals(that.sqlTypeKey);
    }

    @Override
    public int hashCode() {
      return sqlTypeKey.hashCode();
    }

    /**
     * Add an alias
     * It's a shortcut/utility function to be able to add easily an alias by name
     * in {@link com.tabulify.spi.DataSystem#dataTypeBuildingMain(SqlDataTypeManager)}
     * The builder returned is the parent
     */
    public SqlDataTypeBuilder<T> addChildAliasName(String typeName) {
      KeyNormalizer typeNormalized = KeyNormalizer.createSafe(typeName);
      return addChildAliasName(typeNormalized);

    }

    public SqlDataTypeBuilder<T> addChildAliasName(KeyInterface typeName) {
      if (typeName instanceof SqlDataTypeAnsi) {
        throw new InternalException("To add a child alias of a typed name, you should use the add child alias typed name function (addChildAliasTypedName)");
      }
      return addChildAliasName(typeName.toKeyNormalizer());
    }

    public SqlDataTypeBuilder<T> addChildAliasName(KeyNormalizer childName) {

      if (childName.equals(this.sqlTypeKey.toKeyNormalizer())) {
        return this;
      }
      SqlDataTypeBuilder<T> childBuilder = null;
      try {
        childBuilder = manager.getTypeBuilder(childName, javaClazz);
      } catch (Exception e) {
        throw new InternalException("Error while adding the alias (" + childName + ") to the type (" + this + "). Error: " + e.getMessage(), e);
      }
      if (childBuilder == null) {
        SqlDataTypeKey sqlDataTypeKey = new SqlDataTypeKey(connection, childName, this.getTypeCode());
        childBuilder = manager.createTypeBuilder(sqlDataTypeKey, this.getJavaClass());
      }
      childBuilder.setParentBuilder(this);
      return this;

    }


    public SqlDataTypeBuilder<T> addChildAliasTypedName(SqlDataTypeAnsi childName, SqlDataTypeManager sqlDataTypeManager) {

      /**
       * We add the relationship
       * For instance, numeric and decimal are often a synonym
       * The typed name is still a valid type name, so we reused it
       */
      SqlDataTypeBuilder<T> childBuilder = manager.getTypeBuilder(childName.toKeyNormalizer(), getJavaClass());
      if (childBuilder == null) {
        childBuilder = manager.createTypeBuilder(childName.toKeyNormalizer(), this.getTypeCode(), getJavaClass());
      }
      childBuilder
        .setAnsiType(this.sqlDataTypeAnsi)
        .setParentBuilder(this);
      /**
       * The typed name type code is no more mapped to a name
       * We add the new mapping here
       * Not that the typed name may be the same
       * For instance, we may add a `text` name as clob type and `clob` name as alias
       * We check for that
       */
      if (this.sqlDataTypeAnsi != childName) {
        sqlDataTypeManager.addTypeCodeTypeNameMapEntry(childName.getAnsiType(), this.sqlTypeKey);
        sqlDataTypeManager.addJavaClassToTypeRelation(childName.getValueClass(), this.sqlTypeKey);
      }
      return this;
    }

    /**
     * Add the common name of the type name as child
     */
    public SqlDataTypeBuilder<T> addChildAliasCommonNames() {
      return addChildAliases(sqlDataTypeAnsi.getAliases());
    }


    public SqlDataTypeBuilder<T> addChildAliases(List<KeyInterface> commonAliasNames) {
      for (KeyInterface commonName : commonAliasNames) {
        addChildAliasName(commonName);
      }
      return this;
    }


    public SqlDataTypeKey getTypeKey() {
      return this.sqlTypeKey;
    }

    public Class<T> getJavaClass() {
      return this.javaClazz;
    }


    public int getMaxPrecision() {
      return this.maxPrecision;
    }

    public int getMaximumScale() {
      return this.maximumScale;
    }

    public int getMinimumScale() {
      return this.minimumScale;
    }
  }
}
