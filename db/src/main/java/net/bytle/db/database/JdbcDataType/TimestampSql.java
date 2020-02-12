package net.bytle.db.database.JdbcDataType;


import net.bytle.db.model.SqlDataType;

import java.sql.Types;

/**
 *
 */
public class TimestampSql extends SqlDataType {

    protected static final int TYPE_CODE = Types.TIMESTAMP;

    @Override
    public int getTypeCode() {
        return TYPE_CODE;
    }

    @Override
    public String getTypeName() {
        return "TIMESTAMP";
    }

    @Override
    public Class<?> getClazz() {
        return java.sql.Timestamp.class;
    }


}
