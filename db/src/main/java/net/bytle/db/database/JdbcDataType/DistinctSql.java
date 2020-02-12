package net.bytle.db.database.JdbcDataType;

import net.bytle.db.model.SqlDataType;

import java.sql.Types;

/**
 *
 */
public class DistinctSql extends SqlDataType {

    protected static final int TYPE_CODE = Types.DISTINCT;

    @Override
    public int getTypeCode() {
        return TYPE_CODE;
    }

    @Override
    public String getTypeName() {
        return "DISTINCT";
    }

    @Override
    public Class<?> getClazz() {
        return null;
    }



}
