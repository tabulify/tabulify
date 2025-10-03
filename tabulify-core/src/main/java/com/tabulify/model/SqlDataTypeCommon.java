package com.tabulify.model;


import java.sql.Types;

/**
 * A list of common other sql type name
 * We don't make a link to {@link Types}
 * because they are used in {@link Types} as alias name
 * and it would create a recursion init problem (NullPointerException)
 */
public enum SqlDataTypeCommon implements SqlDataTypeKeyInterface {

  INT(java.sql.Types.INTEGER),
  INT1(java.sql.Types.TINYINT),
  INT2(java.sql.Types.SMALLINT),
  // int3
  MEDIUMINT(java.sql.Types.INTEGER),
  // mediumint
  INT3(java.sql.Types.INTEGER),
  INT4(java.sql.Types.INTEGER),
  INT8(java.sql.Types.BIGINT),
  CHAR(java.sql.Types.CHAR),
  VARCHAR(java.sql.Types.VARCHAR),
  LONG_VARCHAR(java.sql.Types.LONGVARCHAR),
  NCHAR(java.sql.Types.NCHAR),
  NVARCHAR(java.sql.Types.NVARCHAR),
  LONG_NVARCHAR(java.sql.Types.LONGNVARCHAR),
  NCLOB(java.sql.Types.NCLOB),
  NUM(java.sql.Types.NUMERIC),
  DEC(java.sql.Types.DECIMAL),
  BOOL(java.sql.Types.BOOLEAN),
  DATETIME(java.sql.Types.TIMESTAMP),
  TIMESTAMPTZ(java.sql.Types.TIMESTAMP_WITH_TIMEZONE),
  TIMETZ(java.sql.Types.TIME_WITH_TIMEZONE),
  TIME_WITHOUT_TIME_ZONE(java.sql.Types.TIME),
  TIMESTAMP_WITHOUT_TIME_ZONE(java.sql.Types.TIMESTAMP),
  FLOAT8(java.sql.Types.DOUBLE),
  FLOAT4(java.sql.Types.REAL),
  VARYING_CHARACTER(java.sql.Types.VARCHAR),
  CHAR_VARYING(java.sql.Types.VARCHAR),
  NATIONAL_CHAR(java.sql.Types.NCHAR),
  NATIONAL_CHAR_VARYING(java.sql.Types.NVARCHAR),
  DOUBLE(java.sql.Types.DOUBLE)
  ;

  private final int typeCode;

  SqlDataTypeCommon(int typeCode) {
    this.typeCode = typeCode;
  }


  @Override
  public String getName() {
    return toKeyNormalizer().toSqlTypeCase();
  }

  @Override
  public String getVendor() {
    return "sql";
  }

  @Override
  public Integer getVendorTypeNumber() {
    return typeCode;
  }

}
