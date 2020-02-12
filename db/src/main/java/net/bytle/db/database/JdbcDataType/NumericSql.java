package net.bytle.db.database.JdbcDataType;

import net.bytle.db.model.SqlDataType;

import java.sql.Types;

/**
 *
 */
public class NumericSql extends SqlDataType {

    protected static final int TYPE_CODE = Types.NUMERIC;

    @Override
    public int getTypeCode() {
        return TYPE_CODE;
    }

    @Override
    public String getTypeName() {
        return "NUMERIC";
    }

    @Override
    public Class<?> getClazz() {
        return java.math.BigDecimal.class;
    }


}
