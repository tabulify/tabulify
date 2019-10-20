package net.bytle.db.jdbc.SqlServer;

import net.bytle.db.database.DataTypeDatabaseAbs;

import java.sql.Types;

/**
 * Created by gerard on 28-11-2015.
 */
class SqlServerDbNumericType extends DataTypeDatabaseAbs {

    static Integer TYPE_CODE = Types.NUMERIC;

    @Override
    public int getTypeCode() {
        return TYPE_CODE;
    }

    @Override
    public String getTypeName() {
        return "NUMERIC";
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
