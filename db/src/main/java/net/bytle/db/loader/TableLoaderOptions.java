package net.bytle.db.loader;

/**
 * Created by gerard on 27-11-2015.
 */
public enum TableLoaderOptions implements TableLoaderOption {


    /**
     * If the table already exists, the records will be added (Default)
     */
    INSERT_RECORDS,

    /**
     * If the records already exists, update it, don't insert it if it doesn't exist
     */
    UPDATE_RECORDS,

    /**
     * If the records already exists, update them and insert them otherwise
     */
    UPSERT_RECORDS,

    /**
     * The same as Upsert (An alias)
     */
    MERGE_RECORDS,

    /**
     * If the table already exists, it will be truncated.
     */
    TRUNCATE_TABLE,

    /**
     * Create a new table. If the table exist, throw an error
     */
    CREATE_TABLE,

    /**
     * Create a new table if it does not exist. If the table exist, doesn't throw an error
     */
    CREATE_TABLE_IF_NOT_EXIST,

    /**
     * Drop the table if it exists and recreate it.
     */
    DROP_TABLE,


}


