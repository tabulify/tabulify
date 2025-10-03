package com.tabulify.gen;

public enum DataGenType {

  /**
   * Get data from a data set
   */
  DATA_SET,
  /**
   * Same as data set but from a prebuild one
   * ie A prebuild data set is an entity
   */
  ENTITY,
  /**
   * Get data from the same data set as parent data set column
   */
  DATA_SET_META,
  /**
   * Get a value from a foreign table via foreign key
   */
  FOREIGN_COLUMN,
  /**
   * Get a random value
   */
  RANDOM,
  /**
   * Get the value from an expression
   */
  EXPRESSION,
  /**
   * Get the attribute value of a tabulify object (data path, ...)
   * (used by the {@link com.tabulify.gen.flow.enrich.EnrichDataPath}
   */
  META,
  /**
   * Get value of a record stream
   * (used by the {@link com.tabulify.gen.flow.enrich.EnrichDataPath}
   */
  DATA_PATH_STREAM,
  /**
   * Get value from a regular expression
   */
  REGEXP,
  /**
   * Generate a sequence
   */
  SEQUENCE,
  /**
   * Generate a histogram
   */
  HISTOGRAM


}
