package net.bytle.db.database.Oracle;

import net.bytle.db.database.DataTypeDatabaseAbs;

import java.sql.Types;

/**
 * Created by gerard on 28-11-2015.
 * // Function
 * //      setString: SetString depending on the argument's size relative to the driver's limits on VARCHAR
 * //      setAsciiStream for a very large ASCII Value
 * //      setUnicodeStream for a very large UNICODE value
 * //      setCharacterStream for a very large UNICODE value with a reader
 */
class OraDbLongType extends DataTypeDatabaseAbs {

    static Integer TYPE_CODE = Types.LONGVARCHAR;

    @Override
    public int getTypeCode() {
        return TYPE_CODE;
    }

    @Override
    public String getTypeName() {
        return "LONG";
    }

    @Override
    public String getCreateStatement(int precision, int scale) {

        return "LONG";

    }

    @Override
    public Class<?> getJavaDataType() {
        return String.class;
    }

    @Override
    public Class<?> getVendorClass() {
        return String.class;
    }

}
