package net.bytle.db.sqlite;

/**
 * DataType in Sqlite are more class of datatype
 * https://www.sqlite.org/datatype3.html
 */
public class SqliteDataType {

    // The value is a NULL value.
    static final String NULL = "NULL";

    // The integer value is a signed integer, stored in 1, 2, 3, 4, 6, or 8 bytes depending on the magnitude of the value.
    static final String INTEGER = "INTEGER";

    // The real value is a floating point value, stored as an 8-byte IEEE floating point number.
    static final String REAL = "REAL";

    //  The value is a text string, stored using the database encoding (UTF-8, UTF-16BE or UTF-16LE).
    static final String TEXT = "TEXT";

    // The Blob value is a blob of data, stored exactly as it was input.
    static final String BLOB = "BLOB";


}
