package com.tabulify.model;

import net.bytle.exception.InternalException;

import java.sql.DatabaseMetaData;

/**
 * We created this enum because a cast of null to int gives the value 0
 * and not 2
 */
public enum SqlDataTypeNullable {
  /**
   * {@link DatabaseMetaData#typeNoNulls}
   */
  NO_NULL(0),
  /**
   * {@link DatabaseMetaData#typeNullable}
   */
  NULLABLE(1),
  /**
   * {@link DatabaseMetaData#typeNullableUnknown}
   */
  NULLABLE_UNKNOWN(2);

  private final int code;

  SqlDataTypeNullable(int code) {
    this.code = code;
  }

  public static SqlDataTypeNullable cast(Integer nullable) {
    if (nullable == null) {
      throw new InternalException("nullable should not be null");
    }
    for (SqlDataTypeNullable sqlDataTypeNullable : SqlDataTypeNullable.values()) {
      if (sqlDataTypeNullable.getCode() == nullable) {
        return sqlDataTypeNullable;
      }
    }
    throw new InternalException("The nullable code (" + nullable + ") is unknown (0,1,2) only");
  }

  public static SqlDataTypeNullable cast(Boolean nullable) {
    if (nullable == null) {
      return NULLABLE_UNKNOWN;
    }
    if (nullable) {
      return NULLABLE;
    }
    return NO_NULL;
  }

  public int getCode() {
    return code;
  }

  public Boolean isNullable() {

    return this != NO_NULL;

  }

}
