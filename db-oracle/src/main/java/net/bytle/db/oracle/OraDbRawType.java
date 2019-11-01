package net.bytle.db.oracle;

import net.bytle.db.database.DataTypeDatabaseAbs;

import java.sql.Types;

/**
 * Created by gerard on 28-11-2015.
 */
class OraDbRawType extends DataTypeDatabaseAbs {

    static Integer TYPE_CODE = Types.VARBINARY;

    @Override
    public int getTypeCode() {
        return TYPE_CODE;
    }

    @Override
    public String getTypeName() {
        return "VARBINARY";
    }

    @Override
    public Class<?> getJavaDataType() {
        return  oracle.sql.RAW.class;
    }

    @Override
    public String getCreateStatement(int precision, int scale) {
        // Bug in a Oracle driver where precision is null in a resultSet
        if (precision == 0) {
            return "RAW(2000)"; //TODO: of the max of the data type
        } else {
            return null;
        }
    }

    @Override
    public Class<?> getVendorClass() {
        return byte[].class;
    }

}
