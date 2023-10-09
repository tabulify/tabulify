package net.bytle.db.transfer;

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
   * Transfer used when the data is from the same sql data store
   */
  UPSERT,
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
  UPSERT_WITH_BIND_VARIABLE,
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
  DELETE;


}
