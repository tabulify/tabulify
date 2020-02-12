package net.bytle.db.database.JdbcDataType;

import net.bytle.db.model.SqlDataType;

import java.sql.Types;

/**
 *
 */
public class FloatSql extends SqlDataType {

    protected static final int TYPE_CODE = Types.FLOAT;

    @Override
    public int getTypeCode() {
        return TYPE_CODE;
    }

    @Override
    public String getTypeName() {
        return "FLOAT";
    }

    @Override
    public Class<?> getClazz() {
        return Double.class;
    }


}
