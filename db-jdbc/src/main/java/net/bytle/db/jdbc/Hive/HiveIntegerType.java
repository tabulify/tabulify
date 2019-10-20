package net.bytle.db.jdbc.Hive;

import net.bytle.db.database.DataTypeDatabaseAbs;

import java.sql.Types;

/**
 * Created by gerard on 28-11-2015.
 */
class HiveIntegerType extends DataTypeDatabaseAbs {

    static Integer TYPE_CODE = Types.INTEGER;

    @Override
    public int getTypeCode() {
        return TYPE_CODE;
    }

    @Override
    public String getTypeName() {
        return "INT";
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
