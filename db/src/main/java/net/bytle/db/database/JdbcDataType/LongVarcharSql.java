package net.bytle.db.database.JdbcDataType;

import net.bytle.db.model.SqlDataType;

import java.sql.Types;

/**
 *
 * // Function
 * //      setString: SetString depending on the argument's size relative to the driver's limits on VARCHAR
 * //      setAsciiStream for a very large ASCII Value
 * //      setUnicodeStream for a very large UNICODE value
 * //      setCharacterStream for a very large UNICODE value with a reader
 *
 * LONGVARCHAR represents a large, variable-length character string
 *
 * The JDBC types CHAR, VARCHAR, and LONGVARCHAR are closely related.
 */
public class LongVarcharSql extends SqlDataType {

    protected static final int TYPE_CODE = Types.LONGVARCHAR;

    @Override
    public int getTypeCode() {
        return TYPE_CODE;
    }

    @Override
    public String getTypeName() {
        return "LONGVARCHAR";
    }

    @Override
    public Class<?> getClazz() {
        return String.class;
    }


}
