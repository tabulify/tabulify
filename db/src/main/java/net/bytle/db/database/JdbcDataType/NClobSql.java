package net.bytle.db.database.JdbcDataType;

import net.bytle.db.model.SqlDataType;

import java.sql.Types;

/**
 *
 */
public class NClobSql extends SqlDataType {

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
    public Class<?> getClazz() {
        return java.sql.NClob.class;
    }


}
