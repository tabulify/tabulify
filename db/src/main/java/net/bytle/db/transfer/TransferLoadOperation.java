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



}


