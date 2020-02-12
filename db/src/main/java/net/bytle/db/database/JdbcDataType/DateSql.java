package net.bytle.db.database.JdbcDataType;

import net.bytle.db.model.SqlDataType;

import java.sql.Types;

/**
 *
 */
public class DateSql extends SqlDataType {

    protected static final int TYPE_CODE = Types.DATE;

    @Override
    public int getTypeCode() {
        return TYPE_CODE;
    }

    @Override
    public String getTypeName() {
        return "DATE";
    }


    @Override
    public Class<?> getClazz() {
        return java.sql.Date.class;
    }


}
