package net.bytle.db.jdbc.SqlServer;

import net.bytle.db.database.DataTypeDatabaseAbs;

import java.sql.Types;

/**
 * Created by gerard on 28-11-2015.
 */
class SqlServerDbDecimalType extends DataTypeDatabaseAbs {

    static Integer TYPE_CODE = Types.DECIMAL;

    @Override
    public int getTypeCode() {
        return TYPE_CODE;
    }

    @Override
    public String getTypeName() {
        return "DECIMAL";
    }

    @Override
    public String getCreateStatement(int precision, int scale) {

        // Default will take over
        return null;

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
