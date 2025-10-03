package com.tabulify.oracle;

import com.tabulify.model.SqlDataTypeAnsi;
import com.tabulify.model.SqlDataTypeCommon;
import com.tabulify.model.SqlDataTypeVendor;
import net.bytle.type.KeyInterface;
import net.bytle.type.KeyNormalizer;

import java.sql.Types;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static com.tabulify.oracle.OracleSystem.*;

public enum OracleSqlType implements SqlDataTypeVendor {

  // character varying and char varying are an alias
  // https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/Data-Types.html#GUID-0BC16006-32F1-42B1-B45E-F27A494963FF
  VARCHAR2(
    SqlDataTypeAnsi.CHARACTER_VARYING,
    "varchar2",
    List.of(SqlDataTypeAnsi.CHARACTER_VARYING, SqlDataTypeCommon.CHAR_VARYING),
    0,
    0,
    0, 0),
  /**
   * Integer is not an Oracle Data Type (is not in the list return by the driver)
   * but it can be used because of the ANSI data type mapping
   * (ie integer = NUMERIC(precision,0))
   * <a href="https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/Data-Types.html#GUID-0BC16006-32F1-42B1-B45E-F27A494963FF">...</a>
   * For those data types, the scale (s) defaults to 0.
   */
  NUMBER_INTEGER(
    SqlDataTypeAnsi.INTEGER,
    "number",
    List.of(
      SqlDataTypeCommon.INT,
      SqlDataTypeAnsi.INTEGER
    ),
    0,
    0,
    0,
    0),
  NUMBER_SMALLINT(
    SqlDataTypeAnsi.SMALLINT,
    "number",
    List.of(
      SqlDataTypeAnsi.SMALLINT
    ),
    0,
    0,
    0,
    0),
  /**
   * <a href="https://docs.oracle.com/cd/B28359_01/server.111/b28285/sqlqr06.htm#CHDJJEEA">...</a>
   * We don't use the class oracle.sql.NUMBER.class
   * because the data generator does not know it
   * NUMERIC and DECIMAL are aliases of number
   * <a href="https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/Data-Types.html#GUID-0BC16006-32F1-42B1-B45E-F27A494963FF">...</a>
   */
  NUMBER_NUMERIC(
    SqlDataTypeAnsi.NUMERIC,
    "number",
    List.of(SqlDataTypeAnsi.NUMERIC, SqlDataTypeAnsi.DECIMAL),
    MAX_PRECISION_NUMERIC,
    MAXIMUM_SCALE_NUMERIC,
    MINIMUM_SCALE_NUMERIC,
    0),
  /**
   * NATIONAL CHARACTER VARYING(n) and NATIONAL CHAR VARYING(n) are aliases
   * <a href="https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/Data-Types.html#GUID-0BC16006-32F1-42B1-B45E-F27A494963FF">...</a>
   */
  NVARCHAR2(
    SqlDataTypeAnsi.NATIONAL_CHARACTER_VARYING,
    "nvarchar2",
    List.of(SqlDataTypeAnsi.NATIONAL_CHARACTER_VARYING, SqlDataTypeCommon.NATIONAL_CHAR_VARYING),
    0,
    0,
    0, 0),
  /**
   * NATIONAL CHARACTER(n) and NATIONAL CHAR(n) are aliases
   * <a href="https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/Data-Types.html#GUID-0BC16006-32F1-42B1-B45E-F27A494963FF">...</a>
   */
  NCHAR(
    SqlDataTypeAnsi.NATIONAL_CHARACTER,
    "nchar",
    List.of(SqlDataTypeAnsi.NATIONAL_CHARACTER, SqlDataTypeCommon.NATIONAL_CHAR),
    0,
    0,
    0,
    0),
  // character is an alias
  // https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/Data-Types.html#GUID-0BC16006-32F1-42B1-B45E-F27A494963FF
  CHAR(
    SqlDataTypeAnsi.CHARACTER,
    "char",
    List.of(SqlDataTypeAnsi.CHARACTER, SqlDataTypeCommon.NATIONAL_CHAR),
    0,
    0,
    0,
    0),
  /**
   * Date is a type that stores date and time (timestamp) but has no precisions
   * The DATE data type stores date and time information.
   * See diagram:
   * <a href="https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/Data-Types.html#GUID-A3C0D836-BADB-44E5-A5D4-265BA5968483__GUID-0EA41E53-451F-4ECE-8523-5FC4C5A37977">...</a>
   * See doc:
   * <a href="https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/Data-Types.html#SQLRF-GUID-5405B652-C30E-4F4F-9D33-9A4CB2110F1B">...</a>
   * For each DATE value, Oracle stores the following information: year, month, day, hour, minute, and second.
   * There are 2 type coupled to Oracle date
   * * Oracle Date / Jdbc time
   * * Oracle Date / Jdbc timestamp
   * but not date, we do it here
   */
  DATE_DATE(
    SqlDataTypeAnsi.DATE,
    "date",
    null,
    0,
    0,
    0,
    0
  ),
  DATE_TIMESTAMP(
    SqlDataTypeAnsi.TIMESTAMP,
    "date",
    null,
    0,
    0,
    0,
    0),
  /**
   * Time
   * No time data type but the driver returns DATE/{@link Types#TIME}
   * We use it
   */
  DATE_TIME(
    SqlDataTypeAnsi.TIME,
    "date",
    null,
    0,
    0,
    0,
    0
  ),
  /**
   * Timestamp default and max here
   * <a href="https://docs.oracle.com/en/database/oracle/oracle-database/19/sqlrf/Data-Types.html#SQLRF-GUID-94A82966-D380-4583-9AF1-AEE681881E64">...</a>
   */
  TIMESTAMP(
    SqlDataTypeAnsi.TIMESTAMP,
    "timestamp",
    null,
    9,
    0,
    0,
    6
  ),
  /**
   * CLOB
   * https://docs.oracle.com/javadb/10.10.1.2/ref/rrefclob.html
   */
  ;


  private final SqlDataTypeAnsi ansi;
  private final KeyNormalizer name;
  private final List<KeyInterface> aliases;
  private final int maxPrecision;
  private final int maxScale;
  private final int minScale;
  private final int defaultPrecision;

  OracleSqlType(SqlDataTypeAnsi sqlDataTypeAnsi, String typeName, List<KeyInterface> aliases, int maxPrecision, int maxScale, int minScale, int defaultPrecision) {
    this.ansi = sqlDataTypeAnsi;
    this.name = KeyNormalizer.createSafe(typeName);
    this.aliases = Objects.requireNonNullElse(aliases, Collections.emptyList());
    this.maxPrecision = maxPrecision;
    this.maxScale = maxScale;
    this.minScale = minScale;
    this.defaultPrecision = defaultPrecision;
  }

  @Override
  public Class<?> getValueClass() {
    return ansi.getValueClass();
  }

  @Override
  public int getMaxPrecision() {
    return this.maxPrecision;
  }

  @Override
  public int getMaximumScale() {
    return this.maxScale;
  }

  @Override
  public SqlDataTypeAnsi getAnsiType() {
    return ansi;
  }

  @Override
  public String getDescription() {
    return ansi.getDescription();
  }

  @Override
  public List<KeyInterface> getAliases() {
    return aliases;
  }

  @Override
  public String getName() {
    return name.toSqlTypeCase();
  }

  @Override
  public String getVendor() {
    return "oracle";
  }

  @Override
  public Integer getVendorTypeNumber() {
    return ansi.getVendorTypeNumber();
  }

  @Override
  public int getMinScale() {
    return minScale;
  }

  @Override
  public int getDefaultPrecision() {
    return defaultPrecision;
  }


  @Override
  public KeyNormalizer toKeyNormalizer() {
    return this.name;
  }


}
