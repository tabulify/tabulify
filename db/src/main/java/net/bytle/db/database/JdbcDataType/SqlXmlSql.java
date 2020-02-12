package net.bytle.db.database.JdbcDataType;

import net.bytle.db.model.SqlDataType;

import java.sql.Types;

/**
 *
 */
public class SqlXmlSql extends SqlDataType {

    protected static final int TYPE_CODE = Types.SQLXML;

    @Override
    public int getTypeCode() {
        return TYPE_CODE;
    }

    @Override
    public String getTypeName() {
        return  "SQLXML";
    }

    @Override
    public Class<?> getClazz() {
        return java.sql.SQLXML.class;
    }


}
