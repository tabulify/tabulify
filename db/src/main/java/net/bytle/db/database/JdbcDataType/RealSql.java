package net.bytle.db.database.JdbcDataType;

import net.bytle.db.model.SqlDataType;

import java.sql.Types;

/**
 *
 */
public class RealSql extends SqlDataType {

    protected static final int TYPE_CODE = Types.REAL;

    @Override
    public int getTypeCode() {
        return TYPE_CODE;
    }

    @Override
    public String getTypeName() {
        return "REAL";
    }

    @Override
    public Class<?> getClazz() {
        return Float.class;
    }


}
