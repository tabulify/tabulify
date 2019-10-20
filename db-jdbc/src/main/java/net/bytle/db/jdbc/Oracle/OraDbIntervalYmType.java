package net.bytle.db.jdbc.Oracle;

import net.bytle.db.database.DataTypeDatabaseAbs;
import oracle.jdbc.OracleTypes;

/**
 * Created by gerard on 28-11-2015.
 */
class OraDbIntervalYmType extends DataTypeDatabaseAbs {

    static Integer TYPE_CODE = OracleTypes.INTERVALYM;

    @Override
    public int getTypeCode() {
        return TYPE_CODE;
    }

    @Override
    public String getTypeName() {
        return "INTERVAL_YEAR_MONTH";
    }


    @Override
    public String getCreateStatement(int precision, int scale) {
        return "INTERVAL YEAR ("+precision+") TO MONTH";
    }
    @Override
    public Class<?> getJavaDataType() {
        return oracle.sql.INTERVALYM.class;
    }

    @Override
    public Class<?> getVendorClass() {
        return oracle.sql.INTERVALYM.class;
    }

}
