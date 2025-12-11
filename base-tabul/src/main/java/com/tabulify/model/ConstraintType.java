package com.tabulify.model;

/**
 * Some database needs the type of constraint
 * the name is not unique
 * (I see you mySQL)
 */
public enum ConstraintType {

  // Foreign key constraints
  FOREIGN_KEY,
  // primary key constraints:
  PRIMARY_KEY,
  // unique constraints (index normally)
  UNIQUE_KEY,
  // check constraints
  CHECK

}
