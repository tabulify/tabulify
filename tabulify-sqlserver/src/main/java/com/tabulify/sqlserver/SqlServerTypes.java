package com.tabulify.sqlserver;

import com.tabulify.model.SqlDataTypeAnsi;
import com.tabulify.model.SqlDataTypeVendor;
import microsoft.sql.DateTimeOffset;
import net.bytle.type.KeyInterface;
import net.bytle.type.KeyNormalizer;

import java.sql.Types;
import java.util.List;


public enum SqlServerTypes implements SqlDataTypeVendor {

  /**
   * Alias for row Version (not timestamp)
   * Yolo Timestamp is not a timestamp
   * It's a synonym for row version
   * To record a date or time, use a datetime2 data type.
   * <a href="https://learn.microsoft.com/en-us/sql/t-sql/data-types/rowversion-transact-sql">...</a>
   * Row Version is not supported, is not in the driver, we add it here for info
   */
  TIMESTAMP(
    Types.BINARY,
    SqlDataTypeAnsi.BINARY,
    0,
    0,
    null,
    null),
  /**
   * Rowversion is a data type that put a timestamp number stored in binary on a row
   * The old name was timestamp ???
   * <a href="https://learn.microsoft.com/en-us/sql/t-sql/data-types/rowversion-transact-sql">...</a>
   */
  ROWVERSION(
    Types.BINARY,
    SqlDataTypeAnsi.BINARY,
    0, 0,
    List.of(SqlServerTypes.TIMESTAMP),
    null),
  /**
   * deprecated, but still used
   */
  TEXT(
    Types.LONGVARCHAR,
    SqlDataTypeAnsi.LONG_CHARACTER_VARYING,
    0,
    0,
    null,
    null),
  /**
   * timestamp = datetime2
   * default precision as seen here
   * <a href="https://learn.microsoft.com/en-us/sql/t-sql/data-types/datetime2-transact-sql">...</a>
   * Datetime and Datetime2 have Timestamp has type
   * We set Datetime2 as the priority 1 to win over datetime for a {@link Types#TIMESTAMP}
   * <a href="https://docs.microsoft.com/en-us/sql/t-sql/data-types/datetime-transact-sql?view=sql-server-ver15">...</a>
   * <a href="https://docs.microsoft.com/en-us/sql/t-sql/data-types/datetime2-transact-sql?view=sql-server-ver15">...</a>
   */
  DATETIME2(Types.TIMESTAMP,
    SqlDataTypeAnsi.TIMESTAMP,
    7,
    7,
    null, null),
  /**
   * <a href="https://docs.microsoft.com/en-us/sql/t-sql/data-types/datetimeoffset-transact-sql?view=sql-server-ver15">...</a>
   * The microsoft class is {@link DateTimeOffset}
   * TIMESTAMP_WITH_TIMEZONE is known as datetimeoffset
   * <a href="https://docs.microsoft.com/en-us/sql/t-sql/data-types/datetimeoffset-transact-sql?view=sql-server-ver15">...</a>
   * Type must stay to other ....
   * Otherwise, the driver returns an error: com.microsoft.sqlserver.jdbc.SQLServerException: The conversion from DATETIMEOFFSET to TIMESTAMP_WITH_TIMEZONE is unsupported.
   * Default as seen in the table here
   * <a href="https://learn.microsoft.com/en-us/sql/t-sql/data-types/datetimeoffset-transact-sql">...</a>
   */
  DATETIMEOFFSET(
    microsoft.sql.Types.DATETIMEOFFSET,
    SqlDataTypeAnsi.TIMESTAMP_WITH_TIME_ZONE,
    0,
    7,
    null,
    DateTimeOffset.class
  ),
  /**
   * Float, Double, Real
   * <a href="https://docs.microsoft.com/en-us/sql/t-sql/data-types/float-and-real-transact-sql?view=sql-server-ver15">...</a>
   * Double = float(53) - 53 being the default precision, all good
   * <p>
   * In SQL Server,
   * a double is a float(53)
   * a real is a float(24) - the real name is known and exists !
   * <p>
   * The default value of n is 53.
   * Ref: <a href="https://learn.microsoft.com/en-us/sql/t-sql/data-types/float-and-real-transact-sql">...</a>
   */
  FLOAT(
    Types.DOUBLE,
    SqlDataTypeAnsi.DOUBLE_PRECISION,
    53,
    53,
    List.of(SqlDataTypeAnsi.DOUBLE_PRECISION),
    null),
  /**
   * XML
   * <a href="https://docs.microsoft.com/en-us/sql/connect/jdbc/using-advanced-data-types">...</a>
   * The driver support it and returns a {@link Types#LONGNVARCHAR}
   */
  XML(
    Types.LONGNVARCHAR,
    SqlDataTypeAnsi.XML,
    0,
    0,
    null,
    String.class),
  CHAR(
    Types.CHAR,
    SqlDataTypeAnsi.CHARACTER,
    8000,
    0,
    null,
    null),
  VARCHAR(
    Types.VARCHAR,
    SqlDataTypeAnsi.CHARACTER_VARYING,
    8000,
    0,
    null,
    null),
  /**
   * JSON
   * <p>
   * <a href="https://learn.microsoft.com/en-us/sql/t-sql/data-types/json-data-type">...</a>
   * Only in SQL Server 2025 (17.x) Preview.
   * <a href="https://learn.microsoft.com/en-us/sql/relational-databases/json/store-json-documents-in-sql-tables">...</a>
   * Because the JSON jdbc data type asked is a string, see below url
   * <a href="https://docs.microsoft.com/en-us/sql/connect/jdbc/using-advanced-data-types">...</a>
   * It should work without any modification
   */
  /**
   * Not present in the driver but supported
   * <a href="https://learn.microsoft.com/en-us/sql/t-sql/data-types/int-bigint-smallint-and-tinyint-transact-sql?view=sql-server-ver17"></a>
   */
  TINYINT(
    Types.TINYINT,
    SqlDataTypeAnsi.TINYINT,
    0,
    0,
    null,
    null);

  private final Integer typeCode;
  private final KeyNormalizer name;
  private final SqlDataTypeAnsi ansiType;
  private final int maxPrecision;
  private final int defaultPrecision;
  private final List<KeyInterface> aliases;
  private final Class<?> clazz;

  SqlServerTypes(Integer i, SqlDataTypeAnsi typeAnsi, int maxPrecision, int defaultPrecision, List<KeyInterface> aliases, Class<?> clazz) {

    this.typeCode = i;
    this.name = KeyNormalizer.createSafe(this.name());
    this.ansiType = typeAnsi;
    this.maxPrecision = maxPrecision;
    this.defaultPrecision = defaultPrecision;
    this.aliases = aliases == null ? List.of() : aliases;
    this.clazz = clazz;

  }


  @Override
  public String getName() {
    return name.toSqlTypeCase();
  }

  @Override
  public String getVendor() {
    return "SqlServer";
  }

  @Override
  public Integer getVendorTypeNumber() {
    return typeCode;
  }


  @Override
  public Class<?> getValueClass() {
    if (clazz != null) {
      return clazz;
    }
    return ansiType.getValueClass();
  }

  @Override
  public int getMaxPrecision() {
    return maxPrecision;
  }

  @Override
  public int getMaximumScale() {
    return 0;
  }

  @Override
  public SqlDataTypeAnsi getAnsiType() {
    return this.ansiType;
  }

  @Override
  public String getDescription() {
    return this.ansiType.getDescription();
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
