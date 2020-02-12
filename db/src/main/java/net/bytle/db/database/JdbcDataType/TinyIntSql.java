package net.bytle.db.database.JdbcDataType;

import net.bytle.db.model.SqlDataType;

import java.sql.Types;

/**
 *
 * byte if primitive
 */
public class TinyIntSql extends SqlDataType {

    protected static final int TYPE_CODE = Types.TINYINT;

    @Override
    public int getTypeCode() {
        return TYPE_CODE;
    }

    @Override
    public String getTypeName() {
        return "TINYINT";
    }

    @Override
    public Class<?> getClazz() {
        return Integer.class;
    }

}
