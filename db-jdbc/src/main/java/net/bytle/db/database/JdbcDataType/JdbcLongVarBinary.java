package net.bytle.db.database.JdbcDataType;

import net.bytle.db.database.DataTypeJdbc;
import net.bytle.db.database.DataTypeJdbcAbs;

import java.sql.Types;

/**
 * Created by gerard on 28-11-2015.
 *
 * Function:
 *    - setBytes depending on the argument's size relative to the driver's limits on
 *    - setBinaryStream
 *
 * The JDBC types BINARY, VARBINARY, and LONGVARBINARY are closely related.
 * LONGVARBINARY represents a large, variable-length binary value
 */
public class JdbcLongVarBinary extends DataTypeJdbcAbs implements DataTypeJdbc {

    protected static final int TYPE_CODE = Types.LONGVARBINARY;

    @Override
    public int getTypeCode() {
        return TYPE_CODE;
    }

    @Override
    public String getTypeName() {
        return "LONGVARBINARY";
    }

    @Override
    public Class<?> getJavaDataType() {
        return byte[].class;
    }


}
