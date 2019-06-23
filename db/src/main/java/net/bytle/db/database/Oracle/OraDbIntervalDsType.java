package net.bytle.db.database.Oracle;

import net.bytle.db.database.DataTypeDatabaseAbs;
import oracle.jdbc.OracleTypes;

/**
 * Created by gerard on 28-11-2015.
 */
class OraDbIntervalDsType extends DataTypeDatabaseAbs {

    static Integer TYPE_CODE = OracleTypes.INTERVALDS;

    @Override
    public int getTypeCode() {
        return TYPE_CODE;
    }

    @Override
    public String getTypeName() {
        return "INTERVALDS";
    }

    @Override
    public String getCreateStatement(int precision, int scale) {
        return "INTERVAL DAY ("+precision+") TO SECOND ("+scale+ ")";
    }
    @Override
    public Class<?> getJavaDataType() {
        return oracle.sql.INTERVALDS.class;
    }

    @Override
    public Class<?> getVendorClass() {
        return oracle.sql.INTERVALDS.class;
    }

}
