package com.tabulify.model;

import com.tabulify.type.KeyInterface;

public enum SqlDataTypeAttribute implements KeyInterface {

  NAME,
  ALIASES,
  ANSI_TYPE,
  MAX_PRECISION,
  MIN_SCALE,
  MAX_SCALE,
  CLASS,
  SUPPORTED,
  DESCRIPTION,
  JDBC_CODE,
  /**
   * The {@link java.sql.JDBCType} name
   */
  JDBC_NAME,

  AUTO_INCREMENT,

  FIXED_PRECISION_SCALE,
  PARAMETERS,
  /**
   * Does this type is an unsigned type (ie only positive value)
   */
  UNSIGNED

}
