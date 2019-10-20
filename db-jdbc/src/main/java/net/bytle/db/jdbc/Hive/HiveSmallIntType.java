package net.bytle.db.jdbc.Hive;

import net.bytle.db.database.DataTypeDatabaseAbs;

import java.sql.Types;

/**
 *
 */
class HiveSmallIntType extends DataTypeDatabaseAbs {

    static Integer TYPE_CODE = Types.SMALLINT;

    @Override
    public int getTypeCode() {
        return TYPE_CODE;
    }

    @Override
    public String getTypeName() {
        return "SMALLINT";
    }

    @Override
    public String getCreateStatement(int precision, int scale) {

        // No precision
        // https://cwiki.apache.org/confluence/display/Hive/LanguageManual+Types#LanguageManualTypes-IntegralTypes(TINYINT,SMALLINT,INT/INTEGER,BIGINT)
        return getTypeName();

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
