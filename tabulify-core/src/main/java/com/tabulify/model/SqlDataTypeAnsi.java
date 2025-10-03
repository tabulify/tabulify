package com.tabulify.model;

import net.bytle.exception.InternalException;
import net.bytle.type.KeyInterface;
import net.bytle.type.KeyNormalizer;

import java.sql.JDBCType;
import java.sql.Types;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static com.tabulify.model.SqlDataTypeManager.*;

/**
 * ANSI type are the counter-part of {@link Types}
 * We don't use {@link Types}, we use {@link SqlDataTypeAnsi}
 * This is an extended named version of the {@link JDBCType}
 * <p>
 * One {@link SqlDataTypeKey data type} has one {@link SqlDataTypeAnsi}
 * <p>
 * Types:
 * * All jdbc type
 * * Added: JSON, JSONB and MEDIUMINT
 * * the integer type are signed as the JDBC type, but we may add unsigned type in the future
 * <p>
 * We use them everywhere as a mean to:
 * * define the type of data (ie an oracle date column (YYYY-MM-DD HH:MM:SS) with ansi date data (YYYY-MM-DD)
 * * standardize the types between databases
 * * to pass name and type code
 * * to wrap underlying data type to create new one (for instance, sqlite does not have a json, but we implement it)
 * * for type conversion between database
 * <p>
 * For the java class, the source is TABLE B-1 Mapping from Java Object Types to JDBC Types
 * in the JDBC 4.1 Specification JSR 221, section Data Type Conversion Table (no section number at the end as annex)
 * <a href="https://download.oracle.com/otn-pub/jcp/jdbc-4_1-mrel-spec/jdbc4.1-fr-spec.pdf">...</a>
 * See as example also the
 * <a href="https://javadoc.io/static/com.mysql/mysql-connector-j/9.0.0/index.html?com/mysql/cj/MysqlType.html">com.mysql.cj.MysqlType</a>
 * class
 * Example of usage: a boolean may be stored in a `bit(1)` but also in a `boolean`
 * SqlDataTypeAnsi will be a boolean
 */
public enum SqlDataTypeAnsi implements SqlDataTypeVendor {

  /**
   * Character data
   */
  CHARACTER(
    Types.CHAR,
    String.class,
    SqlDataTypePriority.STANDARD,
    "Fixed-length blank padded character string",
    Collections.singletonList(SqlDataTypeCommon.CHAR),
    true,
    null,
    0),
  CHARACTER_VARYING(
    Types.VARCHAR,
    String.class,
    SqlDataTypePriority.STANDARD_TOP, // for string.class
    "Variable-length character string",
    List.of(SqlDataTypeCommon.VARCHAR, SqlDataTypeCommon.VARYING_CHARACTER),
    true,
    null,
    0),
  /**
   * It is identical to VARCHAR, except that you do not have to specify a maximum length when creating columns of this type.
   */
  LONG_CHARACTER_VARYING(
    Types.LONGVARCHAR,
    String.class,
    SqlDataTypePriority.STANDARD,
    "Very long variable-length character string",
    Collections.singletonList(SqlDataTypeCommon.LONG_VARCHAR),
    true,
    null,
    0),
  CLOB(
    Types.CLOB,
    java.sql.Clob.class,
    SqlDataTypePriority.STANDARD,
    "Very long variable-length character string",
    null,
    true,
    null,
    0),
  // Nchar desc: setNString depending on the argument's size relative to the driver's limits on NVARCHAR
  NATIONAL_CHARACTER(
    Types.NCHAR,
    String.class,
    SqlDataTypePriority.STANDARD,
    "Fixed-length text in Unicode character set",
    Collections.singletonList(SqlDataTypeCommon.NCHAR),
    true,
    null,
    0),
  NATIONAL_CHARACTER_VARYING(
    Types.NVARCHAR,
    String.class,
    SqlDataTypePriority.STANDARD,
    "Variable-length text in a Unicode character set",
    Collections.singletonList(SqlDataTypeCommon.NVARCHAR),
    true,
    null,
    0),
  LONG_NATIONAL_CHARACTER_VARYING(
    Types.LONGNVARCHAR,
    String.class,
    SqlDataTypePriority.STANDARD,
    "Very long variable-length text in a Unicode character set",
    List.of(SqlDataTypeCommon.LONG_NVARCHAR),
    true,
    null,
    0),
  NATIONAL_CLOB(
    Types.NCLOB,
    java.sql.NClob.class,
    SqlDataTypePriority.STANDARD,
    "Very long variable-length in a Unicode character set",
    Collections.singletonList(SqlDataTypeCommon.NCLOB),
    true,
    null,
    0),
  /**
   * The data types NUMERIC, DECIMAL, INTEGER, and SMALLINT
   * are collectively referred to as exact numeric types.
   */
  NUMERIC(Types.NUMERIC, java.math.BigDecimal.class, SqlDataTypePriority.STANDARD, "Exact numeric of selectable precision", Collections.singletonList(SqlDataTypeCommon.NUM), true, null, 0),
  // Using DECIMAL over numeric for {@link java.math.BigDecimal} ie priority = 1
  // Why? Less verbose than NUMERIC, more known, more databases support ???
  DECIMAL(Types.DECIMAL, java.math.BigDecimal.class, SqlDataTypePriority.STANDARD_TOP, "Exact numeric of selectable precision", Collections.singletonList(SqlDataTypeCommon.DEC), true, null, 0),

  /**
   * Bit, binary data
   */
  // BIT(n) creates a fixed-length bit string of exactly n bits
  // They represent sequences of multiple bits
  // Boolean is for a single bit, for N bit, it may be a string, a number, ...
  BIT(Types.BIT, Boolean.class, SqlDataTypePriority.STANDARD, "Fixed-length bit string", null, true, null, 0),
  BINARY(Types.BINARY, byte[].class, SqlDataTypePriority.STANDARD, "Binary data (“byte array”)", null, false, null, 0),
  VARBINARY(Types.VARBINARY, byte[].class, SqlDataTypePriority.STANDARD, "Variable-length binary data (“byte array”)", null, false, null, 0),
  LONG_VARBINARY(Types.LONGVARBINARY, byte[].class, SqlDataTypePriority.STANDARD, "Variable-length binary data (“byte array”)", null, false, null, 0),
  BLOB(Types.BLOB, java.sql.Blob.class, SqlDataTypePriority.STANDARD, "Large binary objects", null, false, null, 0),

  /**
   * Boolean
   */
  BOOLEAN(Types.BOOLEAN, Boolean.class, SqlDataTypePriority.STANDARD_TOP, "Logical Boolean (true/false)", Collections.singletonList(SqlDataTypeCommon.BOOL), true, null, 0),
  /**
   * Exact numeric types
   * (Signed)
   */
  TINYINT(
    Types.TINYINT,
    Byte.class,
    SqlDataTypePriority.STANDARD,
    "One-byte integer (0-255)", Collections.singletonList(SqlDataTypeCommon.INT1),
    true,
    false,
    TINYINT_SIGNED_MAX_LENGTH
  ),
  SMALLINT(
    Types.SMALLINT,
    Short.class,
    SqlDataTypePriority.STANDARD,
    "Two-byte integer",
    Collections.singletonList(SqlDataTypeCommon.INT2),
    true,
    false,
    SMALLINT_SIGNED_MAX_LENGTH
  ),
  MEDIUMINT(
    SqlDataTypeAnsiCode.MEDIUMINT,
    Integer.class,
    SqlDataTypePriority.STANDARD,
    "Three-byte integer",
    Collections.singletonList(SqlDataTypeCommon.INT3),
    true,
    false,
    MEDIUMINT_SIGNED_MAX_LENGTH
  ),
  // Priority for integer is one so that it's chosen
  // above the alias given by the driver
  // Example: for postgres, we get integer, int2, int4
  // We want integer, when we want the integer type for the {@link Types#INTEGER}
  // int is also known as mediumint, int4
  // The keyword INT is a synonym for INTEGER (Mysql, ... https://dev.mysql.com/doc/refman/8.4/en/numeric-types.html)
  INTEGER(
    Types.INTEGER,
    Integer.class,
    SqlDataTypePriority.STANDARD_TOP,
    "Four-byte integer",
    List.of(SqlDataTypeCommon.INT, SqlDataTypeCommon.INT4),
    true,
    false,
    SqlDataTypeManager.INTEGER_SIGNED_MAX_LENGTH
  ),
  BIGINT(
    Types.BIGINT,
    Long.class,
    SqlDataTypePriority.STANDARD_TOP,
    "Eight-byte integer",
    Collections.singletonList(SqlDataTypeCommon.INT8),
    true,
    false,
    BIGINT_SIGNED_MAX_LENGTH
  ),
  /**
   * Approximate numbers
   * The data types FLOAT, REAL, and DOUBLE PRECISION are collectively referred to as approximate numeric types
   */
  // DOUBLE PRECISION is a fixed, standardized type that always uses double-precision floating-point format:
  // Always 64-bit IEEE 754 double precision
  // Always ~15-17 decimal digits of precision
  // Always 53-bit binary mantissa precision
  // Priority TOP because by default, it's zero and the driver may add other double such as money for Postgres
  // DOUBLE PRECISION and not DOUBLE has name because all enum name are SQL name
  DOUBLE_PRECISION(
    Types.DOUBLE,
    Double.class,
    SqlDataTypePriority.STANDARD_TOP,
    "Double precision floating-point number (8 bytes)",
    List.of(SqlDataTypeCommon.DOUBLE, SqlDataTypeCommon.FLOAT8),
    true,
    false,
    0
  ),
  // Float is a non-fixed precision approximate number
  // With float, you can define the precision. ie FLOAT(p)
  // Vs double , with double, you can't. Float is a single-precision (32-bit) while double is a double precision
  // FLOAT(1) to FLOAT(24) maps to single precision (32-bit)
  // FLOAT(25) to FLOAT(53) maps to double precision (64-bit)
  // FLOAT(53) is effectively equivalent to DOUBLE PRECISION
  // Don't be confused with the fact that `Float` is an alias of `Double` for Postgres, they don't support variable float, only fixed float
  // The java class is a double to be able to store a double as specified in the sql reference TABLE B-1 (JDBC Types Mapped to Java Types)
  FLOAT(Types.FLOAT, Double.class, SqlDataTypePriority.STANDARD, "Flexible Precision floating-point number", null, true, false, 0),
  // REAL is a fixed precision (single precision)
  REAL(Types.REAL, Float.class, SqlDataTypePriority.STANDARD, "Single precision floating-point number (4 bytes)", Collections.singletonList(SqlDataTypeCommon.FLOAT4), true, false, 0),


  /**
   * Note from JDBC 4.2, date and time should support the Java 8 Date and Time API(JSR-310)
   * <a href="https://jdbc.postgresql.org/documentation/query/#using-java-8-date-and-time-classes">...</a>
   * {@link java.time.LocalDate} for date
   * {@link java.time.LocalTime} for time
   * {@link LocalDateTime} for timestamp
   * See tables B-4 and B-5 of the JDBC 4.2 specification.
   */
  DATE(Types.DATE, java.sql.Date.class, SqlDataTypePriority.STANDARD, "Calendar date (year, month, day)", null, true, null, 0),

  TIME(Types.TIME, java.sql.Time.class, SqlDataTypePriority.STANDARD, "Time of day without time zone", List.of(SqlDataTypeCommon.TIME_WITHOUT_TIME_ZONE), true, null, 0),
  // No timezone but java.sql.Timestamp store them in UTC
  TIMESTAMP(Types.TIMESTAMP, java.sql.Timestamp.class, SqlDataTypePriority.STANDARD, "Date and time without time zone", List.of(SqlDataTypeCommon.DATETIME, SqlDataTypeCommon.TIMESTAMP_WITHOUT_TIME_ZONE), true, null, 0),
  /**
   * The space between time and zone is not an error, this is the correct
   * sql syntax
   */
  TIMESTAMP_WITH_TIME_ZONE(Types.TIMESTAMP_WITH_TIMEZONE, OffsetDateTime.class, SqlDataTypePriority.STANDARD, "Date and time, including time zone", Collections.singletonList(SqlDataTypeCommon.TIMESTAMPTZ), true, null, 0),
  /**
   * The space between time and zone is not an error, this is the correct
   * sql syntax
   */
  TIME_WITH_TIME_ZONE(Types.TIME_WITH_TIMEZONE, OffsetTime.class, SqlDataTypePriority.STANDARD, "Time of day, including time zone", Collections.singletonList(SqlDataTypeCommon.TIMETZ), true, null, 0),


  /**
   * Hierarchical data
   */
  XML(Types.SQLXML, java.sql.SQLXML.class, SqlDataTypePriority.STANDARD, "XML data", null, true, null, 0),

  /**
   * Json
   * They are added to the ANSI set even if they are not really ANSI
   * Why?
   * * JSON is first citizen type
   * * it's a way for us to be able to test it (Memory resource can get a JSON type, and we can test the translation of a JSON data type from it to another system)
   * * we can tag connection type as being json data
   * * we can select the json type
   */
  JSON(SqlDataTypeAnsiCode.JSON, String.class, SqlDataTypePriority.STANDARD, "Textual JSON data", null, true, null, 0),
  JSONB(SqlDataTypeAnsiCode.JSONB, String.class, SqlDataTypePriority.STANDARD, "Binary JSON data", null, true, null, 0),

  /**
   * An array
   * Example: an array of varchar, an array of integer
   */
  ARRAY(Types.ARRAY, java.sql.Array.class, SqlDataTypePriority.STANDARD, "Ordered collection of values", null, false, null, 0),
  /**
   * STRUCT (also known as ROW type in some contexts) allows you to create composite data types with multiple named fields, similar to records or objects in programming languages.
   */
  STRUCT(Types.STRUCT, java.sql.Struct.class, SqlDataTypePriority.STANDARD, "Structured data with named fields", null, false, null, 0),
  /**
   * REF is used to store references (pointers) to instances of user-defined structured types, enabling object-relational features in SQL databases.RetryClaude can make mistakes. Please double-check responses.
   */
  REF(Types.REF, java.sql.Ref.class, SqlDataTypePriority.STANDARD, "Reference to a structured type", null, false, null, 0),
  DATALINK(Types.DATALINK, java.net.URL.class, SqlDataTypePriority.STANDARD, "Link to external file or resource (URL)", null, true, null, 0),
  ROWID(Types.ROWID, java.sql.RowId.class, SqlDataTypePriority.STANDARD, "Unique row identifier", null, true, null, 0),
  /**
   * Represent the SQL type NULL
   */
  NULL(Types.NULL, Object.class, SqlDataTypePriority.STANDARD, "Null", null, false, null, 0),
  /**
   * The equivalent of {@link Types#OTHER}.
   * It's set by default and show to the user that we can't map it to another type
   */
  OTHER(Types.OTHER, String.class, SqlDataTypePriority.STANDARD, "Database Specific type", null, false, null, 0),
  /**
   * Not supported but added for information
   * The class returned is the underlying Java class
   */
  JAVA_OBJECT(Types.JAVA_OBJECT, Object.class, SqlDataTypePriority.STANDARD, "The underlying Java class", null, false, null, 0),
  /**
   * represents a user-defined distinct type in SQL databases.
   */
  DISTINCT(Types.DISTINCT, Object.class, SqlDataTypePriority.STANDARD, "User-defined distinct type", null, false, null, 0),
  ;


  private final SqlDataTypePriority priority;
  private final int typeCode;
  private final Class<?> javaClass;
  private final String description;
  /**
   * Our shortname that may be used in manifest
   * Note that this name may be not supported by the database
   */
  private final List<KeyInterface> aliases;

  private final boolean isSupported;
  /**
   * Is it an unsigned type
   */
  private final Boolean unsigned;
  private final KeyNormalizer name;

  /**
   * @param type         - the JDBC type code
   * @param javaClass    - the java class
   * @param priority     - when a java class map to multiple sql type, the sql type with higher priority win
   * @param description  - a description
   * @param aliases      - the common shorter name allowed by the SQL specification
   * @param isSupported  - do we support this type
   * @param unsigned     - an unsigned type
   * @param maxPrecision - max precision
   */
  SqlDataTypeAnsi(int type, Class<?> javaClass, SqlDataTypePriority priority, String description, List<KeyInterface> aliases, boolean isSupported, Boolean unsigned, int maxPrecision) {
    this.name = KeyNormalizer.createSafe(this.name());
    this.typeCode = type;
    this.javaClass = javaClass;
    this.priority = priority;
    this.description = description;
    this.isSupported = isSupported;
    this.aliases = Objects.requireNonNullElseGet(
      aliases,
      ArrayList::new
    );
    this.unsigned = unsigned;
  }


  /**
   * Translate by name, type code or together
   *
   * @param typeName the type name
   * @param typeCode the type code
   * @return the type or {@link #OTHER} if not found
   */
  public static SqlDataTypeAnsi cast(KeyNormalizer typeName, Integer typeCode) {

    if (typeName == null && typeCode == null) {
      throw new InternalException("typeName and typeCode can't be null together");
    }

    /**
     * Match by type code.
     * Type match is first because this is the driver of this class
     * (one type, one name)
     * {@link Types#OTHER} is the only type that may have more than one name
     * we skip as this type code is not deterministic
     */
    if (typeCode != null && typeCode != Types.OTHER) {
      for (SqlDataTypeAnsi sqlDataType : SqlDataTypeAnsi.values()) {
        if (sqlDataType.typeCode == typeCode) {
          return sqlDataType;
        }
      }
    }

    /**
     * Match by name
     */
    if (typeName != null) {
      for (SqlDataTypeAnsi sqlDataType : SqlDataTypeAnsi.values()) {
        if (sqlDataType.toKeyNormalizer().equals(typeName)) {
          return sqlDataType;
        }
      }
    }

    return OTHER;

  }


  /**
   * @return the priority between standard name for the same java class
   */
  public SqlDataTypePriority getPriority() {
    return this.priority;
  }

  @Override
  public Class<?> getValueClass() {
    return this.javaClass;
  }

  @Override
  public int getMaxPrecision() {
    return 0;
  }

  @Override
  public int getMaximumScale() {
    return 0;
  }

  @Override
  public SqlDataTypeAnsi getAnsiType() {
    return this;
  }

  public String getDescription() {
    return description;
  }

  @Override
  public String toString() {
    return this.toKeyNormalizer().toSqlTypeCase() + " (" + this.typeCode + ")";
  }

  /**
   * @return known alias names that may be used in manifest
   * Note that this names may be not supported by the database
   */
  public List<KeyInterface> getAliases() {
    return new ArrayList<>(aliases);
  }


  public boolean getIsSupported() {
    return isSupported;
  }

  @Override
  public String getName() {
    return this.toKeyNormalizer().toSqlTypeCase();
  }

  @Override
  public String getVendor() {
    return "java.sql";
  }

  @Override
  public Integer getVendorTypeNumber() {
    return this.typeCode;
  }

  public Boolean getUnsigned() {
    return unsigned;
  }

  @Override
  public KeyNormalizer toKeyNormalizer() {
    return this.name;
  }


}
