package net.bytle.db.database.JdbcDataType;

import net.bytle.db.model.SqlDataType;

import java.sql.Types;

/**
 *
 */
public class IntegerSql extends SqlDataType {

    protected static final int TYPE_CODE = Types.INTEGER;

    @Override
    public int getTypeCode() {
        return TYPE_CODE;
    }

    @Override
    public String getTypeName() {
        return "INTEGER";
    }

    @Override
    public Class<?> getClazz() {
        return Integer.class;
    }


}
