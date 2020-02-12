package net.bytle.db.database.JdbcDataType;

import net.bytle.db.model.SqlDataType;

import java.sql.Types;

/**
 * Created by gerard on 28-11-2015.
 * short if primitive
 */
public class SmallIntSql extends SqlDataType {

    protected static final int TYPE_CODE = Types.SMALLINT;

    @Override
    public int getTypeCode() {
        return TYPE_CODE;
    }

    @Override
    public String getTypeName() {
        return "SMALLINT";
    }

    @Override
    public Class<?> getClazz() {
        return Integer.class;
    }


}
