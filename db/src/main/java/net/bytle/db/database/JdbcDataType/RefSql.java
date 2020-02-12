package net.bytle.db.database.JdbcDataType;

import net.bytle.db.model.SqlDataType;

import java.sql.Ref;
import java.sql.Types;

/**
 *
 * user defined reference
 */
public class RefSql extends SqlDataType {

    protected static final int TYPE_CODE = Types.REF;

    @Override
    public int getTypeCode() {
        return TYPE_CODE;
    }

    @Override
    public String getTypeName() {
        return "REF";
    }

    @Override
    public Class<?> getClazz() {
        return Ref.class;
    }


}
