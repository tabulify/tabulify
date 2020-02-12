package net.bytle.db.database.JdbcDataType;

import net.bytle.db.model.SqlDataType;

import java.sql.Types;

/**
 *
 */
public class JavaObjectSql extends SqlDataType {

    protected static final int TYPE_CODE = Types.JAVA_OBJECT;

    @Override
    public int getTypeCode() {
        return TYPE_CODE;
    }

    @Override
    public String getTypeName() {
        return "JAVA_OBJECT";
    }

    @Override
    public Class<?> getClazz() {
        return Object.class;
    }

}
