package net.bytle.db.database.JdbcDataType;

import net.bytle.db.database.DataTypeJdbc;
import net.bytle.db.database.DataTypeJdbcAbs;

import java.sql.Types;

/**
 * Created by gerard on 28-11-2015.
 * user defined reference
 */
public class JdbcRowid extends DataTypeJdbcAbs implements DataTypeJdbc {

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
    public Class<?> getJavaDataType() {
        return java.sql.RowId.class;
    }

}
