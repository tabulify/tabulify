package net.bytle.db.jdbc.Oracle;

import net.bytle.db.database.DataTypeDatabaseAbs;
import oracle.jdbc.OracleTypes;

/**
 * Created by gerard on 28-11-2015.
 */
class OraDbNumberType extends DataTypeDatabaseAbs {

    static Integer TYPE_CODE = OracleTypes.NUMBER;

    @Override
    public int getTypeCode() {
        return TYPE_CODE;
    }

    @Override
    public String getTypeName() {
        return "NUMBER";
    }

    @Override
    public String getCreateStatement(int precision, int scale) {
        // Bug ? If the scale is -127, it's a float
        if (scale == -127 && precision != 0) {
            return "FLOAT("+precision+")";
        } else {
            // Default will take over
            return null;
        }
    }

    @Override
    public Class<?> getJavaDataType() {
        return oracle.sql.NUMBER.class;
    }

    @Override
    public Class<?> getVendorClass() {
        return oracle.sql.NUMBER.class;
    }

}
