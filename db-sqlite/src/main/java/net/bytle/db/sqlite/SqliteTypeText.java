package net.bytle.db.sqlite;

import net.bytle.db.database.DataTypeDatabaseAbs;

import java.sql.Types;

/**
 * Created by gerard on 28-11-2015.
 * The driver returns TEXT for the VARCHAR type
 * and it makes migrating the data difficult
 */
class SqliteTypeText extends DataTypeDatabaseAbs {

    static Integer TYPE_CODE = Types.VARCHAR;

    @Override
    public int getTypeCode() {
        return TYPE_CODE;
    }

    @Override
    public String getTypeName() {
        return "VARCHAR";
    }

    @Override
    public String getCreateStatement(int precision, int scale) {

        // Default will take over
        return getTypeName()+"("+precision+")";

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
