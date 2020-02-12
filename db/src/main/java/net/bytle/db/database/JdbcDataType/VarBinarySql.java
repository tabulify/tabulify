package net.bytle.db.database.JdbcDataType;

import net.bytle.db.model.SqlDataType;

import java.sql.Types;

/**
 *
 * The JDBC types BINARY, VARBINARY, and LONGVARBINARY are closely related.
 * VARBINARY represents a small, variable-length binary value,
 */
public class VarBinarySql extends SqlDataType {

    protected static final int TYPE_CODE = Types.VARBINARY;

    @Override
    public int getTypeCode() {
        return TYPE_CODE;
    }

    @Override
    public String getTypeName() {
        return "VARBINARY";
    }

    @Override
    public Class<?> getClazz() {
        return byte[].class;
    }

}
