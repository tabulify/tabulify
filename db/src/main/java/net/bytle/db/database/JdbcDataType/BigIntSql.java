package net.bytle.db.database.JdbcDataType;

import net.bytle.db.model.SqlDataType;

import java.sql.Types;

/**
 * Created by gerard on 28-11-2015.
 */
public class BigIntSql extends SqlDataType {

    protected static final int TYPE_CODE = Types.BIGINT;

    @Override
    public int getTypeCode() {
        return TYPE_CODE;
    }

    @Override
    public String getTypeName() {
        return "BIGINT";
    }

    @Override
    public Class<?> getClazz() {
        return long.class;
    }

}
