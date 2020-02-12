package net.bytle.db.database.JdbcDataType;

import net.bytle.db.model.SqlDataType;

import java.sql.Types;

/**
 *
 */
public class ClobSql extends SqlDataType {

    protected static final int TYPE_CODE = Types.CLOB;

    @Override
    public int getTypeCode() {
        return TYPE_CODE;
    }

    @Override
    public String getTypeName() {
        return "CLOB";
    }

    @Override
    public Class<?> getClazz() {
        return java.sql.Clob.class;
    }


}
