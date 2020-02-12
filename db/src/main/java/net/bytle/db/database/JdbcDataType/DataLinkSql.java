package net.bytle.db.database.JdbcDataType;

import net.bytle.db.model.SqlDataType;

import java.sql.Types;

/**
 *
 */
public class DataLinkSql extends SqlDataType {

    protected static final int TYPE_CODE = Types.DATALINK;

    @Override
    public int getTypeCode() {
        return TYPE_CODE;
    }

    @Override
    public String getTypeName() {
        return "DATALINK";
    }

    @Override
    public Class<?> getClazz() {
        return java.net.URL.class;
    }



}
