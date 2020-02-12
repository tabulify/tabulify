package net.bytle.db.database.JdbcDataType;

import net.bytle.db.model.SqlDataType;

import java.sql.Types;

/**
 *
 */
public class DoubleSql extends SqlDataType {

    protected static final int TYPE_CODE = Types.DOUBLE;

    @Override
    public int getTypeCode() {
        return TYPE_CODE;
    }

    @Override
    public String getTypeName() {
        return "DOUBLE";
    }

    @Override
    public Class<?> getClazz() {
        return Double.class;
    }

}
