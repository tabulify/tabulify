package net.bytle.db.database.JdbcDataType;

import net.bytle.db.database.DataTypeJdbc;
import net.bytle.db.database.DataTypeJdbcAbs;

import java.sql.Types;

/**
 * Created by gerard on 28-11-2015.
 * depending on the argument's
 * size relative to the driver's limits on VARCHAR
 * Otherwise LONGVARCHAR
 * Set String
 *
 * VARCHAR represents a small, variable-length character string
 *
 * The JDBC types CHAR, VARCHAR, and LONGVARCHAR are closely related.
 */
public class JdbcVarchar extends DataTypeJdbcAbs implements DataTypeJdbc {

    protected static final int TYPE_CODE = Types.VARCHAR;

    @Override
    public int getTypeCode() {
        return TYPE_CODE;
    }

    @Override
    public String getTypeName() {
        return "VARCHAR";
    }

    @Override
    public Class<?> getJavaDataType() {
        return String.class;
    }

    
}
