package net.bytle.db.oracle;

import net.bytle.db.database.DataTypeDatabaseAbs;

import java.sql.Types;

/**
 * Created by gerard on 28-11-2015.
 * setNString depending on the argument's size relative to the driver's limits on NVARCHAR
 */
class OraDbNVarchar2Type extends DataTypeDatabaseAbs {

    static Integer TYPE_CODE = Types.NVARCHAR;

    @Override
    public int getTypeCode() {
        return TYPE_CODE;
    }

    @Override
    public String getTypeName() {
        return "NVARCHAR2";
    }

    @Override
    public Class<?> getJavaDataType() {
        return String.class;
    }

    @Override
    public Class<?> getVendorClass() {
        return String.class;
    }

}
