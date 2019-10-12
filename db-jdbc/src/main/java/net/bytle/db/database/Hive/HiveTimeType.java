package net.bytle.db.database.Hive;

import net.bytle.db.database.DataTypeDatabaseAbs;

import java.sql.Types;

/**
 * Created by gerard on 28-11-2015.
 */
class HiveTimeType extends DataTypeDatabaseAbs {

    static Integer TYPE_CODE = Types.TIME;

    @Override
    public int getTypeCode() {
        return TYPE_CODE;
    }

    // Time doesn't exist, we try to make it a timestamp
    @Override
    public String getTypeName() {
        return "TIMESTAMP";
    }

    @Override
    public String getCreateStatement(int precision, int scale) {

        return getTypeName();

    }

    @Override
    public Class<?> getJavaDataType() {
        return null;
    }

    @Override
    public Class<?> getVendorClass() {
        return null;
    }

}
