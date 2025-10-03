package com.tabulify.postgres;

import com.tabulify.model.SqlDataTypeAnsi;
import com.tabulify.model.SqlDataTypeCommon;
import com.tabulify.model.SqlDataTypeVendor;
import net.bytle.exception.InternalException;
import net.bytle.type.KeyInterface;
import net.bytle.type.KeyNormalizer;
import org.postgresql.util.PGobject;

import java.sql.Types;
import java.util.List;

/**
 * The postgres type
 */
public enum PostgresDataType implements SqlDataTypeVendor {

  /**
   * CLOB is known as text
   * <a href="https://www.postgresql.org/docs/current/datatype-character.html">...</a>
   */
  TEXT("Variable unlimited length",
    Types.VARCHAR,
    SqlDataTypeAnsi.CHARACTER_VARYING.getValueClass(),
    SqlDataTypeAnsi.CLOB,
    0,
    0, null),
  /**
   * UUID
   * <a href="https://www.postgresql.org/docs/current/datatype-uuid.html
   * "></a>
   * 32 digits
   */
  UUID(
    "Universally Unique Identifiers (UUID)",
    Types.OTHER,
    SqlDataTypeAnsi.CHARACTER.getValueClass(),
    SqlDataTypeAnsi.CHARACTER,
    32, 0, null),
  /**
   * Info-schema-datatypes
   * <a href="https://www.postgresql.org/docs/current/infoschema-datatypes.html">...</a>
   */
  YES_OR_NO("Information schema type: String with YES or NO",
    Types.DISTINCT,
    SqlDataTypeAnsi.CHARACTER_VARYING.getValueClass(),
    SqlDataTypeAnsi.CHARACTER_VARYING,
    3, 0, null),
  CARDINAL_NUMBER("Information schema type: A non-negative integer",
    Types.DISTINCT,
    SqlDataTypeAnsi.INTEGER.getValueClass(),
    SqlDataTypeAnsi.INTEGER,
    0, 0, null),
  CHARACTER_DATA(
    "Information schema type: A character string (without specific maximum length)",
    Types.DISTINCT,
    SqlDataTypeAnsi.CHARACTER_VARYING.getValueClass(),
    SqlDataTypeAnsi.CHARACTER_VARYING,
    0, 0, null),
  // https://stackoverflow.com/questions/74285202/how-to-create-identifier-more-then-63-characters
  // Its length is currently defined as 64 bytes (63 usable characters plus terminator)
  SQL_IDENTIFIER(
    "Information schema type: A character string used for SQL identifiers",
    Types.DISTINCT,
    SqlDataTypeAnsi.CHARACTER.getValueClass(),
    SqlDataTypeAnsi.CHARACTER,
    0, 0, null),
  TIME_STAMP("Information schema type: A domain over the type timestamp with time zone",
    Types.DISTINCT,
    SqlDataTypeAnsi.TIMESTAMP_WITH_TIME_ZONE.getValueClass(),
    SqlDataTypeAnsi.TIMESTAMP_WITH_TIME_ZONE,
    6,
    0,
    null
  ),
  /**
   * NAME
   * <a href="https://www.postgresql.org/docs/current/datatype-character.html#DATATYPE-CHARACTER-SPECIAL-TABLE">...</a>
   * Its length is currently defined as 64 bytes (63 usable characters plus terminator)
   */
  NAME(
    "Internal type for object names",
    Types.VARCHAR,
    SqlDataTypeAnsi.CHARACTER_VARYING.getValueClass(),
    SqlDataTypeAnsi.CHARACTER_VARYING,
    0, 0, null),
  /**
   * Inet support
   * <a href="https://www.postgresql.org/docs/current/datatype-net-types.html">...</a>
   */
  INET(
    "IPv4 and IPv6 hosts and networks",
    Types.OTHER,
    SqlDataTypeAnsi.CHARACTER_VARYING.getValueClass(),
    SqlDataTypeAnsi.CHARACTER_VARYING,
    0, 0, null),
  CIDR(
    "IPv4 and IPv6 networks",
    Types.OTHER,
    SqlDataTypeAnsi.CHARACTER_VARYING.getValueClass(),
    SqlDataTypeAnsi.CHARACTER_VARYING,
    0, 0, null),
  /**
   * Mac addr - Character because the value is fixed
   */
  MACADDR(
    "MAC addresses",
    Types.OTHER,
    SqlDataTypeAnsi.CHARACTER.getValueClass(),
    SqlDataTypeAnsi.CHARACTER,
    0, 0, null),
  /**
   * Mac addr - Character because the value is fixed
   */
  MACADDR8(
    "MAC addresses (EUI-64 format)",
    Types.OTHER,
    SqlDataTypeAnsi.CHARACTER.getValueClass(),
    SqlDataTypeAnsi.CHARACTER, 0, 0, null),
  /**
   * Bpchar is an alias of character
   */
  BPCHAR(
    "Blank-padded char",
    Types.CHAR,
    SqlDataTypeAnsi.CHARACTER.getValueClass(),
    SqlDataTypeAnsi.CHARACTER, 10485760,
    0,
    null
  ),
  /**
   * <a href="https://www.postgresql.org/docs/13/datatype-json.html">...</a>
   * In the documentation, they talk about
   * a {@link PGobject}
   * but a string for json is working
   * The Types is {@link Types#OTHER}
   * To show that we support it, we set the corresponding ANSI Type
   */
  JSON(
    "Textual JSON data",
    Types.OTHER,
    String.class,
    SqlDataTypeAnsi.JSON,
    0,
    0,
    null
  ),
  JSONB(
    "Binary JSON data, decomposed",
    // Sometimes, it's STRUCT completely strange,
    // if we debug the getTypeInfo, we see a OTHER
    Types.OTHER,
    String.class,
    SqlDataTypeAnsi.JSONB,
    0, 0, null),
  // int4 is what the driver returns, not integer
  INTEGER(
    SqlDataTypeAnsi.INTEGER.getDescription(),
    Types.INTEGER,
    SqlDataTypeAnsi.INTEGER.getValueClass(),
    SqlDataTypeAnsi.INTEGER,
    0,
    0,
    List.of(
      SqlDataTypeCommon.INT,
      SqlDataTypeCommon.INT4
    )
  ),
  // int8 is what the driver returns, not bigint
  // but bigint is the value stored in the information schema
  BIGINT(
    SqlDataTypeAnsi.BIGINT.getDescription(),
    Types.BIGINT,
    SqlDataTypeAnsi.BIGINT.getValueClass(),
    SqlDataTypeAnsi.BIGINT,
    0,
    0,
    List.of(SqlDataTypeCommon.INT8)
  ),
  // int2 is what the driver returns, not smallint
  // but smallint is the public value (ie in information_schema.columns)
  SMALLINT(
    SqlDataTypeAnsi.SMALLINT.getDescription(),
    Types.SMALLINT,
    SqlDataTypeAnsi.SMALLINT.getValueClass(),
    SqlDataTypeAnsi.SMALLINT,
    0,
    0,
    List.of(SqlDataTypeCommon.INT2)
  ),
  /**
   * Floating point
   * From <a href="https://www.postgresql.org/docs/13/datatype-numeric.html">...</a>
   * the driver return the alias float4
   * but the value in information_schema.columns is real
   */
  REAL(
    SqlDataTypeAnsi.REAL.getDescription(),
    Types.REAL,
    SqlDataTypeAnsi.REAL.getValueClass(),
    SqlDataTypeAnsi.REAL,
    0,
    0,
    List.of(SqlDataTypeCommon.FLOAT4)
  ),
  // float8 is what the driver returns, not double
  // but the value in information_schema.columns is double precision
  DOUBLE_PRECISION(
    SqlDataTypeAnsi.DOUBLE_PRECISION.getDescription(),
    Types.DOUBLE,
    SqlDataTypeAnsi.DOUBLE_PRECISION.getValueClass(),
    SqlDataTypeAnsi.DOUBLE_PRECISION,
    0,
    0,
    List.of(SqlDataTypeCommon.FLOAT8)
  ),
  /**
   * timestamptz is what the driver returns, not ts with time zone
   * but the value in information_schema.columns is timestamp with time zone
   * <a href="https://www.postgresql.org/docs/9.1/datatype-datetime.html">...</a>
   * timestamptz is set as if it was a timestamp with {@link Types#TIMESTAMP}
   */
  TIMESTAMP_WITH_TIME_ZONE(
    SqlDataTypeAnsi.TIMESTAMP_WITH_TIME_ZONE.getDescription(),
    Types.TIMESTAMP,
    SqlDataTypeAnsi.TIMESTAMP_WITH_TIME_ZONE.getValueClass(),
    SqlDataTypeAnsi.TIMESTAMP_WITH_TIME_ZONE,
    6,
    0,
    List.of(SqlDataTypeCommon.TIMESTAMPTZ)
  ),
  // timetz is what the driver returns, not `time with time zone`
  // but when reading a table (in information_schema.columns), we get a `time with time zone`
  // The driver returns also the TIME JDBC code
  // We override then the default class
  // https://www.postgresql.org/docs/9.1/datatype-datetime.html
  TIME_WITH_TIME_ZONE(
    SqlDataTypeAnsi.TIME_WITH_TIME_ZONE.getDescription(),
    Types.TIME,
    SqlDataTypeAnsi.TIME_WITH_TIME_ZONE.getValueClass(),
    SqlDataTypeAnsi.TIME_WITH_TIME_ZONE,
    6,
    0,
    List.of(SqlDataTypeCommon.TIMETZ)
  ),
  /**
   * In the driver, we get bool with a {@link Types#BIT}
   * but the value in information_schema.columns is boolean
   * <a href="https://www.postgresql.org/docs/9.1/datatype-boolean.html">...</a>
   * Postgres expect the sql name to be lowercase
   */
  BOOLEAN(
    SqlDataTypeAnsi.BOOLEAN.getDescription(),
    Types.BIT,
    SqlDataTypeAnsi.BOOLEAN.getValueClass(),
    SqlDataTypeAnsi.BOOLEAN,
    1,
    0,
    List.of(SqlDataTypeCommon.BOOL)
  ),
  /**
   * <a href="https://www.postgresql.org/docs/current/datatype-character.html">...</a>
   * If specified, the length n must be greater than zero and cannot exceed 10,485,760.
   * Varchar is returned by the driver but
   * character varying is what the value in information_schema.columns
   */
  CHARACTER_VARYING(
    SqlDataTypeAnsi.CHARACTER_VARYING.getDescription(),
    Types.VARCHAR,
    SqlDataTypeAnsi.CHARACTER_VARYING.getValueClass(),
    SqlDataTypeAnsi.CHARACTER_VARYING,
    10485760,
    0,
    List.of(SqlDataTypeCommon.VARCHAR)
  ),
  /**
   * We choose character as parent because this is the value returned
   * in information_schema.columns
   * Not that when creating a table with char, the driver returns `bpchar` as column type
   */
  CHARACTER(
    "Fixed-length, blank-padded",
    Types.CHAR,
    SqlDataTypeAnsi.CHARACTER.getValueClass(),
    SqlDataTypeAnsi.CHARACTER,
    10485760,
    0,
    List.of(
      PostgresDataType.BPCHAR,
      SqlDataTypeCommon.CHAR
    )
  ),
  /**
   * <a href="https://www.postgresql.org/docs/current/datatype-numeric.html#DATATYPE-NUMERIC-DECIMAL">Max precision and scale</a>
   * <a href="https://www.postgresql.org/docs/13/datatype-numeric.html">...</a>
   * Decimal type is an alias name for numeric
   * it gets the type numeric code
   * <a href="https://www.postgresql.org/docs/7.4/datatype.html#DATATYPE-TABLE">...</a>
   */
  NUMERIC(
    SqlDataTypeAnsi.NUMERIC.getDescription(),
    SqlDataTypeAnsi.NUMERIC.getVendorTypeNumber(),
    SqlDataTypeAnsi.NUMERIC.getValueClass(),
    SqlDataTypeAnsi.NUMERIC,
    1000,
    1000,
    List.of(SqlDataTypeAnsi.DECIMAL)
  ),
  // https://www.postgresql.org/docs/9.1/datatype-datetime.html
  TIME(
    SqlDataTypeAnsi.TIME.getDescription(),
    SqlDataTypeAnsi.TIME.getVendorTypeNumber(),
    SqlDataTypeAnsi.TIME.getValueClass(),
    SqlDataTypeAnsi.TIME,
    0,
    0,
    List.of(SqlDataTypeCommon.TIME_WITHOUT_TIME_ZONE)
  ),
  // https://www.postgresql.org/docs/9.1/datatype-datetime.html
  // This is a "timestamp" (ie without timezone)
  TIMESTAMP(
    SqlDataTypeAnsi.TIMESTAMP.getDescription(),
    SqlDataTypeAnsi.TIMESTAMP.getVendorTypeNumber(),
    SqlDataTypeAnsi.TIMESTAMP.getValueClass(),
    SqlDataTypeAnsi.TIMESTAMP,
    6,
    0,
    List.of(SqlDataTypeCommon.TIMESTAMP_WITHOUT_TIME_ZONE)
  );


  private final String description;
  private final int typeCode;
  private final Class<?> clazz;
  private final SqlDataTypeAnsi ansi;
  private final int maxPrecision;
  private final int maxScale;
  private final List<KeyInterface> aliases;
  private final KeyNormalizer keyNormalizer;

  PostgresDataType(String description, int jdbcTypeCode, Class<?> javaClazz, SqlDataTypeAnsi ansiType, int maxPrecision, int maxScale, List<KeyInterface> aliases) {
    this.description = description;
    this.typeCode = jdbcTypeCode;
    this.clazz = javaClazz;
    this.ansi = ansiType;
    this.maxPrecision = maxPrecision;
    this.maxScale = maxScale;
    this.aliases = aliases == null ? List.of() : aliases;
    this.keyNormalizer = KeyNormalizer.createSafe(this.name());
  }

  public static PostgresDataType cast(KeyNormalizer typeName) {

    if (typeName == null) {
      throw new InternalException("typeName can't be null");
    }

    for (PostgresDataType sqlDataType : PostgresDataType.values()) {
      if (sqlDataType.toKeyNormalizer().equals(typeName)) {
        return sqlDataType;
      }
    }


    return null;

  }

  public String getDescription() {
    return description;
  }


  @Override
  public String getName() {
    return toKeyNormalizer().toSqlTypeCase();
  }

  @Override
  public String getVendor() {
    return "Postgres";
  }

  @Override
  public Integer getVendorTypeNumber() {
    return typeCode;
  }

  public Class<?> getValueClass() {
    return this.clazz;
  }

  public SqlDataTypeAnsi getAnsiType() {
    return this.ansi;
  }

  public int getMaximumScale() {
    return this.maxScale;
  }


  public int getMaxPrecision() {
    return this.maxPrecision;
  }

  public List<KeyInterface> getAliases() {
    return aliases;
  }

  @Override
  public KeyNormalizer toKeyNormalizer() {
    return this.keyNormalizer;
  }
}
