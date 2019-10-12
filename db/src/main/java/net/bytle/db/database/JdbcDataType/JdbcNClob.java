package net.bytle.db.database.JdbcDataType;

import net.bytle.db.database.DataTypeJdbc;
import net.bytle.db.database.DataTypeJdbcAbs;

import java.sql.Types;

/**
 * Created by gerard on 28-11-2015.
 */
public class JdbcNClob extends DataTypeJdbcAbs implements DataTypeJdbc {

    protected static final int TYPE_CODE = Types.NCLOB;

    @Override
    public int getTypeCode() {
        return TYPE_CODE;
    }

    @Override
    public String getTypeName() {
        return "NCLOB";
    }

    @Override
    public Class<?> getJavaDataType() {
        return java.sql.NClob.class;
    }
    

}
