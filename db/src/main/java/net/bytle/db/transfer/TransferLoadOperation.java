package net.bytle.db.transfer;

/**
 * How the data should be loaded in the target document
 */
public enum TransferLoadOperation {


  /**
   * If the table already exists, the records will be added (Default)
   */
  INSERT,

  /**
   * The same as Insert (An alias)
   */
  APPEND,

  /**
   * If the records already exists, update it, don't insert it if it doesn't exist
   */
  UPDATE,

  /**
   * If the records already exists, update them and insert them otherwise
   */
  UPSERT,

  /**
   * The same as Upsert (An alias)
   */
  MERGE,

  /**
   * Move
   *   * rename operation on the same same system
   *   * or transfer operation
   *      * where the target data path will:
   *          * have the same structure (columns),
   *          * contain at the end of the operations the same data set
   *      * where the source data path will be deleted
   */
  MOVE,
  /**
   * Same as move but without deleting the source data path
   */
  COPY;


}


