package net.bytle.db.oracle;

import net.bytle.db.database.DataTypeDatabaseAbs;

import java.sql.Types;

/**
 * Created by gerard on 28-11-2015.
 */
class OraDbDoubleType extends DataTypeDatabaseAbs {

    static Integer TYPE_CODE = Types.DOUBLE;

    @Override
    public int getTypeCode() {
        return TYPE_CODE;
    }

    @Override
    public String getTypeName() {
        return "NUMBER";
    }

    @Override
    public Class<?> getJavaDataType() {
        return Double.class;
    }

    @Override
    public Class<?> getVendorClass() {
        return Double.class;
    }

}
