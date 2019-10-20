package net.bytle.db.jdbc.SqlServer;

import net.bytle.db.database.DataTypeDatabaseAbs;

import java.sql.Types;

/**
 * Created by gerard on 28-11-2015.
 * short if primitive
 */
public class SlqServerDbSmallIntType extends DataTypeDatabaseAbs {

    protected static final int TYPE_CODE = Types.SMALLINT;

    @Override
    public int getTypeCode() {
        return TYPE_CODE;
    }

    @Override
    public String getTypeName() {
        return "SMALLINT";
    }

    @Override
    public Class<?> getJavaDataType() {
        return Integer.class;
    }

    @Override
    public Class<?> getVendorClass() {
        return null;
    }

    @Override
    public String getCreateStatement(int precision, int scale) {
        return "SMALLINT";
    }


}
