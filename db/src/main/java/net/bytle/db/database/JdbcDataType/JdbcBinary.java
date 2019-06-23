package net.bytle.db.database.JdbcDataType;

import net.bytle.db.database.DataTypeJdbc;
import net.bytle.db.database.DataTypeJdbcAbs;

import java.sql.Types;

/**
 * Created by gerard on 28-11-2015.
 *
 * The JDBC types BINARY, VARBINARY, and LONGVARBINARY are closely related.
 * BINARY represents a small, fixed-length binary value
 */
public class JdbcBinary extends DataTypeJdbcAbs implements DataTypeJdbc {

    protected static final int TYPE_CODE = Types.BINARY;

    @Override
    public int getTypeCode() {
        return TYPE_CODE;
    }

    @Override
    public String getTypeName() {
        return "BINARY";
    }

    @Override
    public Class<?> getJavaDataType() {
        return byte[].class;
    }


}
