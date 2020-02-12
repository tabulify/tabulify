package net.bytle.db.database.JdbcDataType;

import net.bytle.db.model.SqlDataType;

import java.sql.Types;

/**
 *
 * user defined object
 */
public class StructSql extends SqlDataType {

    protected static final int TYPE_CODE = Types.STRUCT;

    @Override
    public int getTypeCode() {
        return TYPE_CODE;
    }

    @Override
    public String getTypeName() {
        return "STRUCT";
    }

    @Override
    public Class<?> getClazz() {
        return java.sql.Struct.class;
    }

}
