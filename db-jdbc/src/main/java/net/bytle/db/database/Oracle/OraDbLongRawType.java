package net.bytle.db.database.Oracle;

import net.bytle.db.database.DataTypeDatabaseAbs;

/**
 * Created by gerard on 28-11-2015.
 */
class OraDbLongRawType extends DataTypeDatabaseAbs {

    static Integer TYPE_CODE = -4;

    @Override
    public int getTypeCode() {
        return TYPE_CODE;
    }

    @Override
    public String getTypeName() {
        return "LONG RAW";
    }

    @Override
    public Class<?> getJavaDataType() {
        return oracle.sql.RAW.class;
    }

    @Override
    public String getCreateStatement(int precision, int scale) {

        return "LONG RAW";
    }

    @Override
    public Class<?> getVendorClass() {
        return byte[].class;
    }

}
