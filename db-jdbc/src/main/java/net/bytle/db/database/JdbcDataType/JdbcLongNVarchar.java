package net.bytle.db.database.JdbcDataType;

import net.bytle.db.database.DataTypeJdbc;
import net.bytle.db.database.DataTypeJdbcAbs;

import java.sql.Types;

/**
 * Created by gerard on 28-11-2015.
 * // Function
 * // setNString depending on the argument's size relative to the driver's limits on NVARCHAR
 * // setNCharacterStream
 */
public class JdbcLongNVarchar extends DataTypeJdbcAbs implements DataTypeJdbc {

    protected static final int TYPE_CODE = Types.LONGNVARCHAR;

    @Override
    public int getTypeCode() {
        return TYPE_CODE;
    }

    @Override
    public String getTypeName() {
        return "LONGNVARCHAR";
    }

    @Override
    public Class<?> getJavaDataType() {
        return String.class;
    }


}
