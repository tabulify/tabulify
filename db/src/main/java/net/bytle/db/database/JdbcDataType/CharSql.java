package net.bytle.db.database.JdbcDataType;

import net.bytle.db.model.SqlDataType;

import java.sql.Types;

/**
 * Created by gerard on 28-11-2015.
 * Char represents a small, fixed-length character string
 *
 * The JDBC types CHAR, VARCHAR, and LONGVARCHAR are closely related.
 */
public class CharSql extends SqlDataType {

    protected static final int TYPE_CODE = Types.CHAR;

    @Override
    public int getTypeCode() {
        return TYPE_CODE;
    }

    @Override
    public String getTypeName() {
        return "CHAR";
    }

    @Override
    public Class<?> getClazz() {
        return String.class;
    }


}
