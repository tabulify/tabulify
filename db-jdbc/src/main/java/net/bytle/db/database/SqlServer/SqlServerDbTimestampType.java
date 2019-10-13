package net.bytle.db.database.SqlServer;

import net.bytle.db.database.DataTypeDatabaseAbs;

import java.sql.Types;

/**
 * Created by gerard on 28-11-2015.
 */
public class SqlServerDbTimestampType extends DataTypeDatabaseAbs {

    protected static final int TYPE_CODE = Types.TIMESTAMP;

    @Override
    public int getTypeCode() {
        return TYPE_CODE;
    }

    @Override
    /**
     *
     * The SQL Server driver gives a smalltime but we want a datetime2.
     *
     * Otherwise when going from an Oracle date to a datetime Sql Server we of the following error
     * because the data range is smaller in a smalldatetime than in a datetime2 (ie Oracle Date)
     *
     * Error:
     * The conversion of a datetime2 data type to a smalldatetime data type resulted in an out-of-range value.
     */
    public String getTypeName() {
        return "datetime2";
    }

    @Override
    public Class<?> getJavaDataType() {
        return java.sql.Timestamp.class;
    }

    @Override
    public Class<?> getVendorClass() {
        return null;
    }


}
