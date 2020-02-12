package net.bytle.db.database.JdbcDataType;

import net.bytle.db.model.SqlDataType;

import java.sql.Types;

/**
 * Created by gerard on 28-11-2015.
 *
 * The JDBC types BINARY, VARBINARY, and LONGVARBINARY are closely related.
 * BINARY represents a small, fixed-length binary value
 */
public class BinarySql extends SqlDataType {

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
    public Class<?> getClazz() {
        return byte[].class;
    }


}
