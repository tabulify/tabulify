package net.bytle.db.sqlite;

/**
 *
 * DataType in Sqlite has not rigid typing.
 *   * one column can have several values with different type
 *   * no typecode
 *   * the type description of the create statement is saved as is.
 *   * one column has just an affinity (ie a preference)
 *   * type is by cell and not by columns
 *
 *
 * DataType in Sqlite are declared via affinity that maps to a class storage
 * https://www.sqlite.org/datatype3.html
 *
 * ^ Affinity ^ stores all data using storage classes ^
 * | TEXT     | NULL, TEXT or BLOB
 * | NUMERIC  | NULL, TEXT, BLOB, REAL, INTEGER (ALL)
 * | INTEGER  | Same as NUMERIC (The diff is on the cast expression)
 * | REAL     | Same as NUMERIC except that it forces integer values into floating point representation
 * | BLOB     | BLOB
 *
 * The type affinity of a column is the recommended type for data stored in that column.
 * the type is recommended, not required. It can still be another storage
 * Columns with an affinity will prefer to use the affinity storage
 *
 *
 * The data type string used in the create statement
 * will be preserved but Sqlite has only four storage class
 *
 * Every table column has a type affinity
 * Every cell has also an affinity
 */
public class SqliteTypeAffinity {

    // The value is a NULL value.
    static final String NULL = "NULL";

    // The integer value is a signed integer, stored in 1, 2, 3, 4, 6, or 8 bytes depending on the magnitude of the value.
    // INT, INTEGER, TINYINT, SMALLINT, MEDIUMINT, BIGINT, UNSIGNED BIG INT, INT2, INT8
    static final String INTEGER = "INTEGER";

    // The real value is a floating point value, stored as an 8-byte IEEE floating point number.
    // REAL, FLOA, or DOUB declaration are treated as REAL
    // REAL, DOUBLE, DOUBLE PRECISION, FLOAT
    static final String REAL = "REAL";


    // CHAR, VARCHAR, CLOB or TEXT declaration are treated as TEXT
    // CHARACTER(20), VARCHAR(255), VARYING CHARACTER(255), NCHAR(55), NATIVE CHARACTER(70), NVARCHAR(100), TEXT, CLOB
    // TEXT stores all data using storage classes NULL, TEXT or BLOB
    static final String TEXT = "TEXT";

    // The Blob value is a blob of data, stored exactly as it was input.
    static final String BLOB = "BLOB";

    // NUMERIC, DECIMAL(10,5), BOOLEAN, DATE, DATETIME
    static final String NUMERIC = "NUMERIC";


}
