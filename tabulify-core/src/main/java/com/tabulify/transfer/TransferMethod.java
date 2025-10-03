package com.tabulify.transfer;

/**
 * The transfer method is the actual applied method
 * of a transfer
 */
public enum TransferMethod {

  /**
   * Create table as
   * Transfer used when the data is from the same sql data store
   */
  CREATE_TABLE_AS,
  /**
   * Insert from query
   * Transfer used when the data is from the same sql data store
   */
  INSERT_FROM_QUERY,
  /**
   * A move against the same system is a rename
   * Transfer used when the data is from the same sql data store
   */
  RENAME,
  /**
   * Update sql statement with values
   * Transfer used when the data is from the same sql data store
   */
  UPDATE,
  /**
   * Update from a query
   */
  UPDATE_FROM_QUERY,
  /**
   * Upsert sql statement values based
   */
  UPSERT_MERGE_LITERAL,
  /**
   * Upsert update/insert with literal
   */
  UPSERT_UPDATE_INSERT_WITHOUT_PARAMETERS,
  /**
   * Upsert insert/update with literal
   */
  UPSERT_INSERT_UPDATE_WITHOUT_PARAMETERS,
  /**
   * Upsert sql statement select based
   */
  UPSERT_FROM_QUERY,
  /**
   * Append in file, add in memory structure, insert values in table
   */
  INSERT,
  /**
   * A sql insert with bind variable
   */
  INSERT_WITH_BIND_VARIABLE,
  /**
   * A sql merge with bind variable
   */
  UPSERT_MERGE_WITH_PARAMETERS,
  /**
   * An insert then an update
   */
  UPSERT_INSERT_UPDATE_WITH_PARAMETERS,
  /**
   * An update then an insert
   */
  UPSERT_UPDATE_INSERT_WITH_PARAMETERS,
  /**
   * A sql update with bind variable
   */
  UPDATE_WITH_BIND_VARIABLE,
  /**
   * A sql delete from a select
   */
  DELETE_FROM_QUERY,
  /**
   * A sql delete with bind variable
   */
  DELETE_WITH_BIND_VARIABLE,
  /**
   * A delete with values
   */
  DELETE,


}
