package net.bytle.db.database.JdbcDataType;

import net.bytle.db.model.SqlDataType;

import java.sql.Types;

/**
 *
 * setNString depending on the argument's size relative to the driver's limits on NVARCHAR
 */
public class NVarcharSql extends SqlDataType {

    protected static final int TYPE_CODE = Types.NVARCHAR;

    @Override
    public int getTypeCode() {
        return TYPE_CODE;
    }

    @Override
    public String getTypeName() {
        return "NVARCHAR";
    }

    @Override
    public Class<?> getClazz() {
        return String.class;
    }


}
