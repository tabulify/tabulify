package net.bytle.db.database.JdbcDataType;

import net.bytle.db.model.SqlDataType;

import java.sql.Types;

/**
 *
 */
public class BooleanSql extends SqlDataType {

    protected static final int TYPE_CODE = Types.BOOLEAN;

    @Override
    public int getTypeCode() {
        return TYPE_CODE;
    }

    @Override
    public String getTypeName() {
        return "BOOLEAN";
    }

    @Override
    public Class<?> getClazz() {
        return Boolean.class;
    }



}
