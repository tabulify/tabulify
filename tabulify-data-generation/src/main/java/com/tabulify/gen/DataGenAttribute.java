package com.tabulify.gen;

/**
 * The attribute in yaml for data generator
 */
public enum DataGenAttribute {
  TYPE,
  COLUMN_PARENTS,
  // expression
  EXPRESSION,
  // sequence attribute
  OFFSET, START, RESET, VALUES, MAX_TICK, STEP,
  // histogram
  BUCKETS,
  // data set
  COLUMN, ENTITY, LOCALE, DATA_URI,
  // random
  MIN, MAX, REGEXP,

}
