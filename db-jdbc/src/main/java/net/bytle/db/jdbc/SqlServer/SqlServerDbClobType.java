package net.bytle.db.jdbc.SqlServer;

import net.bytle.db.database.DataTypeDatabaseAbs;

import java.sql.Types;

/**
 * Created by gerard on 28-11-2015.
 */
class SqlServerDbClobType extends DataTypeDatabaseAbs {

    static Integer TYPE_CODE = Types.CLOB;

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

        // SQL Server introduces a max specifier for varchar,
        // nvarchar, and varbinary data types to allow storage of values as large as 2^31 bytes.

            return getTypeName()+"(max)";

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
