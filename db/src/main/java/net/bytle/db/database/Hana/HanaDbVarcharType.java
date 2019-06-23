package net.bytle.db.database.Hana;

import net.bytle.db.database.DataTypeDatabaseAbs;

import java.sql.Types;

/**
 * Created by gerard on 11-01-2016.
 * VARCHAR is having length in bytes (not in CHAR !)
 *
 * The VARCHAR(n) data type specifies a variable-length character string, where n :
 *    * indicates the maximum length in bytes
 *    * and is an integer between 1 and 5000.
 * https://help.sap.com/saphelp_hanaplatform/helpdata/en/20/a1569875191014b507cf392724b7eb/content.htm
 *
 * Example: The following doesn't fit in a VARCHAR(35)
 * select length(TO_VARCHAR('TØJEKSPERTEN HØRSHOLM APS - NR. 252')) from dummy;
 */
public class HanaDbVarcharType extends DataTypeDatabaseAbs {

    protected static final int TYPE_CODE = Types.VARCHAR;

    @Override
    public int getTypeCode() {
        return TYPE_CODE;
    }

    @Override
    public String getTypeName() {
        return "NVARCHAR";
    }

    @Override
    public Class<?> getJavaDataType() {
        return String.class;
    }

    @Override
    public Class<?> getVendorClass() {
        return null;
    }

    @Override
    public String getCreateStatement(int precision, int scale) {

//        int length = precision * 2;
//        if (length > 5000 ) {
//            length = 5000;
//        }
//        return "VARCHAR ("+length+")";
        return "NVARCHAR ("+precision+")";
    }

}
