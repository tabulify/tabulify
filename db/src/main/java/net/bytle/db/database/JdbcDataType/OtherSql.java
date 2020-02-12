package net.bytle.db.database.JdbcDataType;


import net.bytle.db.model.SqlDataType;

import java.sql.Types;

/**
 *
 */
public class OtherSql extends SqlDataType {

    protected static final int TYPE_CODE = Types.OTHER;

    @Override
    public int getTypeCode() {
        return TYPE_CODE;
    }

    @Override
    public String getTypeName() {
        return "OTHER";
    }

    @Override
    public Class<?> getClazz() {
        return null;
    }


}
