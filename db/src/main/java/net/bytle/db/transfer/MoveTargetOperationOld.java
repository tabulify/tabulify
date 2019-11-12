package net.bytle.db.transfer;

/**
 * Move Operations on the target document
 */
public enum MoveTargetOperationOld {



    /**
     * If the document already exists, it will be truncated before loading
     */
    TRUNCATE_IF_EXIST,

    /**
     * Truncate the document before loading
     */
    TRUNCATE,

    /**
     * Create a new document. If the document exist, throw an error
     */
    CREATE,

    /**
     * Create a new document if it does not exist. If the document exist, doesn't throw an error
     */
    CREATE_IF_NOT_EXIST,

    /**
     * Drop the target document if it exist
     */
    DROP_IF_EXIST,

    /**
     * Drop the target document
     */
    DROP,



}


