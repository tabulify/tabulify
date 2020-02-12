package net.bytle.db.database.JdbcDataType;

import net.bytle.db.model.SqlDataType;

import java.sql.Types;

/**
 * Created by gerard on 28-11-2015.
 * user defined reference
 */
public class RowidSql extends SqlDataType {

    protected static final int TYPE_CODE = Types.ROWID;

    @Override
    public int getTypeCode() {
        return TYPE_CODE;
    }

    @Override
    public String getTypeName() {
        return "ROWID";
    }

    @Override
    public Class<?> getClazz() {
        return java.sql.RowId.class;
    }

}
