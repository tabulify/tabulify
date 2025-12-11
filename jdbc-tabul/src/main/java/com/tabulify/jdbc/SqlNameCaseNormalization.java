package com.tabulify.jdbc;

/**
 * When identifier is not quoted
 * {@link SqlConnectionAttributeEnum#NAME_QUOTING_ENABLED}
 * database may apply a casing to maje them case-insensitive
 * This is the case of Oracle that makes them uppercase
 */
public enum SqlNameCaseNormalization {
  NONE,
  UPPERCASE,
  LOWERCASE
}
