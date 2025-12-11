package com.tabulify.mysql;

import com.mysql.cj.MysqlType;
import com.tabulify.model.SqlDataTypeAnsi;
import com.tabulify.model.SqlDataTypeCommon;
import com.tabulify.model.SqlDataTypeVendor;
import com.tabulify.type.KeyInterface;
import com.tabulify.type.KeyNormalizer;

import java.sql.Timestamp;
import java.util.List;

/**
 * My Sql Specifics Name Type that we use in our code
 * so that you can see where they are used
 */
public enum MySqlVendorTypes implements SqlDataTypeVendor {


  ENUM(
    MysqlType.ENUM.getJdbcType(),
    SqlDataTypeAnsi.CHARACTER,
    0,
    null,
    0,
    0,
    String.class,
    "String value chosen from a list of permitted values"
  ),
  SET(
    MysqlType.SET.getJdbcType(),
    SqlDataTypeAnsi.CHARACTER,
    0,
    null,
    0,
    0,
    String.class,
    "String value with zero or more values chosen from a list of defined permitted values"
  ),
  YEAR(
    MysqlType.YEAR.getJdbcType(),
    SqlDataTypeAnsi.DATE,
    4,
    null,
    0,
    0,
    null,
    "1-byte type used to represent year values"
  ),
  /**
   * MySQL has 2 timestamp, Timestamp and datetime
   * Here we give the priority to timestamp
   * datetime has a difference in range with timestamp, not really synonym/alias
   * <a href="https://dev.mysql.com/doc/refman/8.4/en/datetime.html">...</a>
   */
  DATETIME(
    MysqlType.DATETIME.getJdbcType(),
    SqlDataTypeAnsi.TIMESTAMP,
    6,
    null,
    0,
    0,
    // MySQL recommend LocalDateTime.class,
    // but we don't have implemented it in any generator
    // works also with java.sql.timestamp
    SqlDataTypeAnsi.TIMESTAMP.getValueClass(),
    null),

  /**
   * MySQL has 2 timestamp, Timestamp and datetime
   * Here we give the priority to timestamp
   * datetime has a difference in range with timestamp, not really synonym/alias
   * <a href="https://dev.mysql.com/doc/refman/8.4/en/datetime.html">...</a>
   * A DATETIME or TIMESTAMP value can include a trailing fractional seconds part in up to microseconds (6 digits) precision.
   */
  TIMESTAMP(
    MysqlType.TIMESTAMP.getJdbcType(),
    SqlDataTypeAnsi.TIMESTAMP,
    6,
    null,
    0,
    0,
    Timestamp.class,
    null),

  /**
   * VARCHAR column that uses the utf8 character set can be declared to be a maximum of 21,844 characters.
   * <a href="https://dev.mysql.com/doc/refman/5.6/en/string-type-syntax.html">...</a>
   */
  VARCHAR(
    MysqlType.VARCHAR.getJdbcType(),
    SqlDataTypeAnsi.CHARACTER_VARYING,
    // not Math.toIntExact(MysqlType.VARCHAR.getPrecision())
    // because it's way much, 65535L
    21844,
    null,
    2000,
    0,
    null,
    null
  ),
  // LONG and LONG VARCHAR map to the MEDIUMTEXT data type. This is a compatibility feature.
  // Ref: https://dev.mysql.com/doc/refman/8.4/en/blob.html
  LONG_VARCHAR(
    MysqlType.MEDIUMTEXT.getJdbcType(),

    SqlDataTypeAnsi.LONG_CHARACTER_VARYING,
    Math.toIntExact(MysqlType.MEDIUMTEXT.getPrecision()),
    null,
    0, 0, null, null),

  /**
   * TEXT
   * The text type correspond to the four BLOB types and have the same maximum lengths and storage requirements
   * <a href="https://dev.mysql.com/doc/refman/8.4/en/blob.html">...</a>
   */
  TEXT(
    MysqlType.TEXT.getJdbcType(),

    SqlDataTypeAnsi.LONG_CHARACTER_VARYING,
    Math.toIntExact(MysqlType.TEXT.getPrecision()),
    null, 0, 0, null, null),

  TINYTEXT(
    MysqlType.TINYTEXT.getJdbcType(),

    SqlDataTypeAnsi.CHARACTER_VARYING,
    Math.toIntExact(MysqlType.TINYTEXT.getPrecision()),
    null, 0, 0, null, null),

  MEDIUMTEXT(
    MysqlType.MEDIUMTEXT.getJdbcType(),

    SqlDataTypeAnsi.LONG_CHARACTER_VARYING,
    Math.toIntExact(MysqlType.MEDIUMTEXT.getPrecision()),
    List.of(LONG_VARCHAR),
    0, 0, null, null),

  LONGTEXT(
    MysqlType.LONGTEXT.getJdbcType(),

    SqlDataTypeAnsi.LONG_CHARACTER_VARYING,
    // long value, not integer - MysqlType.LONGTEXT.getPrecision()
    0,
    null, 0, 0, null, null),

  // JSON
  JSON(
    MysqlType.JSON.getJdbcType(),

    SqlDataTypeAnsi.JSON,
    Math.toIntExact(MysqlType.JSON.getPrecision()),
    null, 0, 0, null, null),

  // SERIAL is an alias for BIGINT UNSIGNED NOT NULL AUTO_INCREMENT UNIQUE.
  // https://dev.mysql.com/doc/refman/8.4/en/numeric-type-syntax.html
  MEDIUMINT(
    MysqlType.MEDIUMINT.getJdbcType(),

    SqlDataTypeAnsi.MEDIUMINT,
    Math.toIntExact(MysqlType.MEDIUMINT.getPrecision()),
    null, 0, 0, null, null),

  /**
   * <a href="https://dev.mysql.com/doc/refman/5.7/en/char.html">...</a>
   * 256 - 1 (from 0 to 255)
   */
  CHAR(
    MysqlType.CHAR.getJdbcType(),

    SqlDataTypeAnsi.CHARACTER,
    Math.toIntExact(MysqlType.CHAR.getPrecision()),
    null,
    0, 0, null, null),

  /**
   * We build the alias relationship
   * <a href="https://dev.mysql.com/doc/refman/5.7/en/fixed-point-types.html">...</a>
   * the keywords DEC and FIXED are synonyms for DECIMAL (Ref: <a href="https://dev.mysql.com/doc/refman/8.4/en/numeric-types.html">...</a>)
   * numeric = decimal as stated here:
   * <a href="https://dev.mysql.com/doc/refman/5.7/en/fixed-point-types.html">...</a>
   */
  DECIMAL(
    MysqlType.DECIMAL.getJdbcType(),
    SqlDataTypeAnsi.DECIMAL,
    // The maximum number of digits for DECIMAL is 65
    // <a href="https://dev.mysql.com/doc/refman/8.4/en/fixed-point-types.html">...</a>
    Math.toIntExact(MysqlType.DECIMAL.getPrecision()),
    List.of(
      KeyNormalizer.createSafe("fixed"),
      SqlDataTypeCommon.DEC,
      SqlDataTypeAnsi.NUMERIC
    ),
    10,
    // The maximum number of supported decimals (D) is 30.
    // <a href="https://dev.mysql.com/doc/refman/8.4/en/numeric-type-syntax.html#id195932">...</a>
    30,
    null,
    null
  ),

  // Unsigned by default, we override it
  // The type names int2 is an extension of smallint
  // https://dev.mysql.com/doc/refman/8.4/en/integer-types.html
  SMALLINT(
    MysqlType.SMALLINT.getJdbcType(),

    SqlDataTypeAnsi.SMALLINT,
    Math.toIntExact(MysqlType.SMALLINT.getPrecision()),
    List.of(KeyNormalizer.createSafe("smallint signed")),
    0,
    0,
    null,
    null),

  /**
   * <a href="https://dev.mysql.com/doc/refman/8.4/en/integer-types.html">...</a>
   */
  BIGINT(
    MysqlType.BIGINT.getJdbcType(),

    SqlDataTypeAnsi.BIGINT,
    Math.toIntExact(MysqlType.BIGINT.getPrecision()),
    List.of(KeyNormalizer.createSafe("bigint signed")),
    0,
    0,
    null,
    null),

  /**
   * <a href="https://dev.mysql.com/doc/refman/8.4/en/integer-types.html">...</a>
   */
  TINYINT(
    MysqlType.TINYINT.getJdbcType(),

    SqlDataTypeAnsi.TINYINT,
    Math.toIntExact(MysqlType.TINYINT.getPrecision()),
    null,
    0,
    0,
    null,
    null),

  /**
   * <a href="https://dev.mysql.com/doc/refman/8.4/en/integer-types.html">...</a>
   */
  INTEGER(
    MysqlType.INT.getJdbcType(),

    SqlDataTypeAnsi.INTEGER,
    Math.toIntExact(MysqlType.INT.getPrecision()),
    List.of(
      SqlDataTypeCommon.INT,
      KeyNormalizer.createSafe("int signed"),
      KeyNormalizer.createSafe("integer signed")
    ),
    0,
    0,
    null,
    null),

  /**
   * <a href="https://dev.mysql.com/doc/refman/8.4/en/integer-types.html">...</a>
   */
  INTEGER_UNSIGNED(
    MysqlType.INT_UNSIGNED.getJdbcType(),

    SqlDataTypeAnsi.INTEGER,
    Math.toIntExact(MysqlType.INT_UNSIGNED.getPrecision()),
    List.of(
      KeyNormalizer.createSafe("int unsigned")
    ),
    0,
    0,
    null,
    "Four-byte integer unsigned"),

  /**
   * <a href="https://dev.mysql.com/doc/refman/8.4/en/integer-types.html">...</a>
   */
  MEDIUMINT_UNSIGNED(
    MysqlType.MEDIUMINT_UNSIGNED.getJdbcType(),

    SqlDataTypeAnsi.MEDIUMINT,
    Math.toIntExact(MysqlType.MEDIUMINT_UNSIGNED.getPrecision()),
    null,
    0,
    0,
    null,
    null),

  /**
   * Strange, strange
   * BOOLEAN in the doc is only found in the <a href="https://dev.mysql.com/doc/refman/8.4/en/other-vendor-data-types.html">conversion page</a> where they say to use tinyint
   * but bool is part of the driver
   * boolean works also and as it's the ANSI name, we choose it as parent
   * creating a boolean column statement, create a tinyint(1)
   */
  BOOLEAN(
    MysqlType.BOOLEAN.getJdbcType(),

    SqlDataTypeAnsi.BOOLEAN,
    Math.toIntExact(MysqlType.TINYINT.getPrecision()),
    List.of(SqlDataTypeCommon.BOOL),
    0,
    0,
    null,
    null),

  /**
   * <a href="https://dev.mysql.com/doc/refman/8.4/en/time.html">...</a>
   */
  TIME(
    MysqlType.TIME.getJdbcType(),

    SqlDataTypeAnsi.TIME,
    Math.toIntExact(MysqlType.TIME.getPrecision()),
    null,
    0,
    0,
    null,
    null),

  /**
   * The names are already present in the driver, we just add them to build the alias relationship
   * Approximate numerics
   * MySQL also treats REAL as a synonym for DOUBLE PRECISION (a nonstandard variation),
   * unless the REAL_AS_FLOAT SQL mode is enabled.
   * <a href="https://dev.mysql.com/doc/refman/8.4/en/floating-point-types.html">...</a>
   */
  DOUBLE_PRECISION(
    MysqlType.DOUBLE.getJdbcType(),
    SqlDataTypeAnsi.DOUBLE_PRECISION,
    Math.toIntExact(MysqlType.DOUBLE.getPrecision()),
    List.of(
      SqlDataTypeCommon.DOUBLE,
      SqlDataTypeAnsi.REAL
    ),
    0,
    0,
    null,
    null),

  DOUBLE_PRECISION_UNSIGNED(
    MysqlType.DOUBLE_UNSIGNED.getJdbcType(),
    SqlDataTypeAnsi.DOUBLE_PRECISION,
    Math.toIntExact(MysqlType.DOUBLE_UNSIGNED.getPrecision()),
    List.of(KeyNormalizer.createSafe("double unsigned")),
    0,
    0,
    null,
    "Double precision unsigned - floating-point number (8 bytes)"),

  /**
   * float supported
   */
  FLOAT(
    // The type code is {@link Types#REAL}
    MysqlType.FLOAT.getJdbcType(),
    SqlDataTypeAnsi.FLOAT,
    Math.toIntExact(MysqlType.FLOAT.getPrecision()),
    null,
    0,
    0,
    Float.class,
    null
  ),
  ;


  /**
   * From where to copy the type properties
   */
  private final SqlDataTypeAnsi ansi;
  private final int typeCode;
  private final int maxPrecision;
  private final List<KeyInterface> aliases;
  private final int defaultPrecision;
  private final int maxScale;
  private final Class<?> valueClazz;
  private final String description;


  MySqlVendorTypes(int jdbcType,
                   SqlDataTypeAnsi ansi,
                   int maxPrecision,
                   List<KeyInterface> aliases,
                   int defaultPrecision,
                   int maxScale,
                   Class<?> valueClass,
                   String description) {

    this.ansi = ansi;
    this.typeCode = jdbcType;
    this.maxPrecision = maxPrecision;
    this.aliases = aliases == null ? List.of() : aliases;
    this.defaultPrecision = defaultPrecision;
    this.maxScale = maxScale;
    this.valueClazz = valueClass != null ? valueClass : ansi.getValueClass();
    this.description = description;

  }


  @Override
  public String getName() {
    return this.toKeyNormalizer().toSqlTypeCase();
  }

  @Override
  public String getVendor() {
    return "MySQL";
  }


  public Integer getVendorTypeNumber() {
    return this.typeCode;
  }

  @Override
  public Class<?> getValueClass() {
    return this.valueClazz;
  }

  @Override
  public int getMaxPrecision() {
    return maxPrecision;
  }

  @Override
  public int getMaximumScale() {
    return maxScale;
  }

  @Override
  public SqlDataTypeAnsi getAnsiType() {
    return ansi;
  }

  @Override
  public String getDescription() {
    if (description != null) {
      return description;
    }
    return ansi.getDescription();
  }

  @Override
  public List<KeyInterface> getAliases() {
    return aliases;
  }

  @Override
  public int getDefaultPrecision() {
    return defaultPrecision;
  }


}
