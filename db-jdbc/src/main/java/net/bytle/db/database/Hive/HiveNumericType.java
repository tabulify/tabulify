package net.bytle.db.database.Hive;

import net.bytle.db.database.DataTypeDatabaseAbs;

import java.sql.Types;

/**
 * Numeric exist in 3.0
 * https://cwiki.apache.org/confluence/display/Hive/LanguageManual+Types#LanguageManualTypes-decimal
 */
class HiveNumericType extends DataTypeDatabaseAbs {

    static Integer TYPE_CODE = Types.NUMERIC;

    @Override
    public int getTypeCode() {
        return TYPE_CODE;
    }

    @Override
    public String getTypeName() {
        return "DECIMAL";
    }

    @Override
    public String getCreateStatement(int precision, int scale) {

        return getTypeName()+"("+precision+","+scale+")";

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
