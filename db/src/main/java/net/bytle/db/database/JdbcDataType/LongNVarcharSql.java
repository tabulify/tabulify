package net.bytle.db.database.JdbcDataType;

import net.bytle.db.model.SqlDataType;

import java.sql.Types;

/**
 *
 * // Function
 * // setNString depending on the argument's size relative to the driver's limits on NVARCHAR
 * // setNCharacterStream
 */
public class LongNVarcharSql extends SqlDataType {

    protected static final int TYPE_CODE = Types.LONGNVARCHAR;

    @Override
    public int getTypeCode() {
        return TYPE_CODE;
    }

    @Override
    public String getTypeName() {
        return "LONGNVARCHAR";
    }

    @Override
    public Class<?> getClazz() {
        return String.class;
    }


}
