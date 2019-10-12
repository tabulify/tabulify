package net.bytle.db.database.JdbcDataType;

import net.bytle.db.database.DataTypeJdbc;
import net.bytle.db.database.DataTypeJdbcAbs;

import java.sql.Types;

/**
 * Created by gerard on 28-11-2015.
 * setNString depending on the argument's size relative to the driver's limits on NVARCHAR
 */
public class JdbcNChar extends DataTypeJdbcAbs implements DataTypeJdbc {

    protected static final int TYPE_CODE = Types.NCHAR;

    @Override
    public int getTypeCode() {
        return TYPE_CODE;
    }

    @Override
    public String getTypeName() {
        return "NCHAR";
    }

    @Override
    public Class<?> getJavaDataType() {
        return String.class;
    }


}
